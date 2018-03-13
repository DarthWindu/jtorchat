package core;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

import util.ConfigWriter;
import util.Status;
import util.StatusUtils;
import util.Tray;


public class Buddy {
    
    private static Random random = new Random();
    private String address;
    private String name; // wtf is this for?, custom name? todo use this somewhere -- used
    private volatile Socket ourSock = null; // Our socket to them - the output sock
    private volatile OutputStreamWriter ourSockOut = null;
    private volatile Socket theirSock = null; // Their socket to us - the input sock
    private String cookie;
    private String theirCookie;
    private byte status = Status.OFFLINE;
    private int connectFailCount = 0;
    private boolean receivedPong;
    private boolean sentPong;
    private String profile_name;
    private String client = "";
    
    private String version = "";
    private String profile_text;
    private Object connectLock = new Object();
    
    private long connectedAt;
    private long connectTime;
    private long reconnectAt;
    private long lastPing = -1;
    private long lastStatus = -1;
    private long lastStatusRecieved;
    private int unansweredPings;
    
    private int npe1Count;
    
    public Buddy (String address, String name, Boolean now) {
        
        this.address = address;
        this.name = name;
        
        this.cookie = generateCookie();
        
        if (now) // Prevent flooding
        {
            BuddyList.addBuddy(this);
        }
    }
    
    public static String generateCookie () {
        String cookie = "";
        String alphaNumeric = "abcdefghijklmnopqrstuvwxyz1234567890";
        for (int i = 0; i < 77; i++)
            cookie += alphaNumeric.charAt(random.nextInt(alphaNumeric.length()));
        return cookie;
    }
    
    
    public void connect () {
        if (!getBlack(this.address)) {
            if (ourSock != null) {
                reconnectAt = -1;
                Logger.log(Logger.WARNING, this, "Connect(V)V was called but ourSock isn't null!");
                Thread.dumpStack();
                return;
            }
            // maybe store sock connection in another variable then move it to
            // ourSock when connected - fixed with ourSockOut
            ThreadManager.registerWork(ThreadManager.NORMAL, () -> {
                if (!getBlack(Buddy.this.address)) {
                    try {
                        reconnectAt = -1;
                        connectTime = System.currentTimeMillis();
                        ourSock = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", Config.SOCKS_PORT)));
                        ourSock.connect(InetSocketAddress.createUnresolved(address + ".onion", 11009));
                        setStatus(Status.HANDSHAKE);
                        Logger.log(Logger.INFO, Buddy.this, "Connected to {" + address + ", " + name + "}");
                        // ourSockOut = ourSock.getOutputStream();
                        ourSockOut = new OutputStreamWriter(ourSock.getOutputStream(), "UTF-8");
                        sendPing();
                        if (theirCookie != null) {
                            sendPong(theirCookie);
                            Logger.log(Logger.DEBUG, Buddy.this, "Sent " + address + " a cached pong");
                            theirCookie = null; // cbb to clear properly elsewhere
                        }
                        connectTime = -1;
                        if (ourSock == null)
                            Logger.log(Logger.SEVERE, Buddy.this, "Wtf?! ourSock is null, but we just connected");
                        connectFailCount = 0;
                        // System.err.println(ourSockOut);
                    } catch (Exception e) {
                        connectFailCount++;
                        if (ourSock != null)
                            try {
                                ourSock.close();
                            } catch (IOException e1) {
                                // we should'nt have to worry about this
                            }
                        ourSock = null;
                        ourSockOut = null;
                        setStatus(Status.OFFLINE);
                        Logger.log(Logger.WARNING, Buddy.this, "Failed to connect to " + address + " : " + e.getMessage() + " | Retry in " + (reconnectAt - System.currentTimeMillis()));
                        // e.printStackTrace();
                        connectTime = -1;
                    }
                    
                    synchronized (connectLock) {
                        connectLock.notifyAll(); // incase something messed up we clear it out of the way so it should work next time
                    }
                    if (ourSockOut != null) {
                        try {
                            InputStream is = ourSock.getInputStream();
                            byte b;
                            String input = "";
                            while ((b = (byte) is.read()) != -1) {
                                if ((char) b == '\n') { // shouldnt happen
                                    Logger.log(Logger.SEVERE, Buddy.this.getClass(), "Recieved unknown '" + input + "' on ourSock from " + Buddy.this.toString(true));
                                    input = "";
                                    continue;
                                }
                                input += (char) b;
                                if ((char) b == ' ' && !input.substring(0, input.length() - 1).contains(" ")) {
                                    BuddyIncoming.init_outin(input, Buddy.this, is);
                                    input = "";
                                }
                            }
                            
                            Logger.log(Logger.SEVERE, Buddy.this.getClass(), "BROKEN - " + address);
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                disconnect();
                            } catch (IOException ioe) {
                                // ignored
                            }
                        }
                    }
                }
            }, "Connect to " + address, "Connection thread for " + address);
        }
    }
    
    public void setmyStatus (int status) {
        
        switch (status) {
            case 1:
                TCPort.status = "available";
                break;
            
            case 2:
                TCPort.status = "away";
                break;
            
            case 3:
                TCPort.status = "xa";
                break;
        }
        
        Logger.log(Logger.INFO, "Buddy", "Status set " + TCPort.status);

        /*
            try {
                sendStatus();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        this might be useful later (???)
        */
    }
    
    public void sendPing () throws IOException {
        String command = "ping";
        sendRaw(command + " " + Config.us + " " + cookie);//IOException can occur here
        unansweredPings++;//Umm, always? Maybe I'm misunderstanding
        if (status == Status.OFFLINE || status == Status.HANDSHAKE)
            lastPing = System.currentTimeMillis();
    }
    
    public void sendRaw (String command) throws IOException {
        Object OSO_LOCK = new Object(); // OurSock Outputstream lock
        //Object TSO_LOCK = new Object(); // TheirSock Outputstream lock
        //Scanner ourScanner;
        
        
        if (!getBlack(this.address)) {
            synchronized (OSO_LOCK) {
                try {
                    Logger.log(Logger.DEBUG, this, "Send " + address + " " + command);
                    ourSockOut.write((command + ((char) 10)));
                    ourSockOut.flush();
                } catch (IOException e) {
                    Logger.log(Logger.WARNING, this, "[" + address + "] ourSock = null; theirSock = null; " + e.getLocalizedMessage());
                    disconnect();
                    throw e;
                }
            }
        }
    }
    
    public void sendPong (String pong) throws IOException {
        sendRaw("pong " + pong);
        sentPong = true;
    }
    
    public void sendClient () throws IOException {
        sendRaw("client " + Config.CLIENT);
    }
    
    public void sendVersion () throws IOException {
        sendRaw("version " + Config.VERSION);
    }
    
    public void sendProfileName () throws IOException {
        sendRaw("profile_name " + TCPort.profile_name);
    }
    
    public void sendProfileText () throws IOException {
        sendRaw("profile_text " + TCPort.profile_text);
    }
    
    public void sendAddMe () throws IOException {
        sendRaw("add_me");
    }
    
    public void sendStatus () throws IOException {

	    /*
	    Previous If condition (exact)
	        (Config.updateStatus > 0 & Config.updateStatus < 4)
	     */
        if (Config.updateStatus > 0 && Config.updateStatus < 4) {
            setmyStatus(Config.updateStatus);
            Config.updateStatus = 0;
        }
        
        
        sendRaw("status " + TCPort.status);
        lastStatus = System.currentTimeMillis();
        
    }
    
    
    public void onFullyConnected () throws IOException {
        sendClient();
        sendVersion();
        sendProfileName();
        sendProfileText();
        sendAddMe();
        sendStatus();
        
        
        if (!BuddyList.buds.containsKey(this.address))
            BuddyList.addBuddy(this);
        
    }
    
    public void attach (Socket socket, Scanner sc) throws IOException {
        if (!getBlack(this.address)) {
            if (theirSock != null) {
                disconnect();
                connect(); // TODO might need to do something about this entire block
                synchronized (connectLock) {
                    try {
                        Logger.log(Logger.NOTICE, this, "Waiting...");
                        // connectLock.wait(45000); // 45sec wait for conenct
                        connectLock.wait(); // wait for conenct
                        // !NOTE! notify is called regardless of success or failure
                    } catch (InterruptedException e) {
                        //TODO REMOVE OR DO SOMETHING WITH THIS EMPTY CATCH BLOCK
                        //Maybe log the exception?
                    }
                }
            }
            if (status == Status.OFFLINE && connectTime != -1) {
                // connect() method is trying to connect atm
                
                // is severe so its printed on err
                Logger.log(Logger.SEVERE, this, "status == Status.OFFLINE && connectTime != -1");
				/*
				synchronized(connectLock) {
				try {
				// connectLock.wait(45000); // 45sec wait for conenct
				Logger.log(Logger.NOTICE, this, "Waiting...");
				connectLock.wait(); // wait for conenct
				// !NOTE! notify is called regardless of success or failure
				} catch (InterruptedException e) { }
				}
				*/
            }
            // FIXME really need to fix replying to commands before we're connected
            this.theirSock = socket;
            this.receivedPong = false;
            try {
                while (sc.hasNext()) {
                    if (!getBlack(this.address)) {
                        String next = sc.next();
                        if (!sentPong && theirCookie != null) {
                            try {
                                sendPong(theirCookie);
                            } catch (NullPointerException npe) {
                                Logger.log(Logger.INFO, Buddy.this, "1Caught npe on " + address);
                                if (npe1Count++ > 5) {
                                    disconnect();
                                    connect();
                                    return;
                                }
                            }
                        }
                        BuddyIncoming.init(next, this);
                    }
                }
            } catch (SocketException se) {
                Logger.log(Logger.DEBUG, this, "[" + address + "] attach() " + se.getLocalizedMessage() + " | " + se.getStackTrace()[0]);
                // SocketExceptions are quite common and generally nothing to worry about
            } catch (IOException e) {
                Logger.log(Logger.WARNING, this, "[" + address + "] theirSock = null; ourSock = null; " + e.getLocalizedMessage() + " | " + e.getStackTrace()[0]);
                disconnect();
                throw e;
            }
        }
    }
    
    public void sendMessage (String string) throws IOException {
        sendRaw("message " + string);
    }
    
    public void sendDisconnect () throws IOException {
        sendRaw("disconnect");
    }
    
    public long getTimeSinceLastStatus () {
        return System.currentTimeMillis() - lastStatus;
    }
    
    @Override
    public String toString () {
        return (name != null && name.length() > 0) ? name :
               (profile_name != null && profile_name.length() > 0) ? profile_name :
               "[" + address + "]"; // + " (" + address + ")";
    }
    
    public String toString (boolean b) {
        /*
        TODO This method returns a boolean, but it's cast to String. Check if it's appropriate to change return type.
        */
        
        return address.equals(Config.us) ? language.langtext[61] :
               (profile_name != null && profile_name.length() > 0) ? profile_name + " (" + address + ")" :
               (name != null && name.length() > 0) ? name + " (" + address + ")" :
               "[" + address + "]"; // + " (" + address + ")";
    }
    
    
    
    public void disconnect () throws IOException { // should be used with caution
        if (ourSock != null)
            ourSock.close();
        ourSock = null;
        if (theirSock != null)
            theirSock.close();
        theirSock = null;
        setStatus(Status.OFFLINE);
        Logger.log(Logger.NOTICE, this, "Disconnect called on " + address + " | Retry in " + (reconnectAt - System.currentTimeMillis()));
    }
    
    public void remove () throws IOException {
        ConfigWriter.deletebuddy(this);
        BuddyList.buds.remove(this.address);
        try {
            if (this.isFullyConnected()) {
                this.sendRaw("remove_me");
            }
        } catch (IOException e) {
            //TODO REMOVE OR DO SOMETHING IN THIS EMPTY CATCH BLOCK
        }
        disconnect();
        APIManager.fireBuddyRemoved(this);
    }
    
    //GETTERS AND SETTERS
    public byte getStatus () {
        return status;
    }
    
    protected void setStatus (byte status) {
        if (status == Status.OFFLINE) {
            // if (address.equals("jutujsy2ufg33ckl"))
            // Thread.dumpStack();
            reconnectAt = connectFailCount < 4 ? (15) : connectFailCount < 15 ? (5 * 60) : (30 * 60);
            reconnectAt *= (7 + random.nextInt(13)) / 10d;
            reconnectAt *= 1000;
            reconnectAt += System.currentTimeMillis();
            sentPong = false;
            receivedPong = false;
            unansweredPings = 0;
            ourSockOut = null;
            
            if (this.status >= Status.ONLINE) {
                Logger.log(Logger.INFO, this, address + " connected for " + (connectedAt == -1 ? " Not Set" :
                                                                             ((System.currentTimeMillis() - connectedAt) / 1000)));
                connectedAt = -1;
            }
        }
        
        //NOTE A: THE DIFFERENCE BETWEEN status and this.status in the if conditions
        noteA:
        {
            // During handshake lastStatus is used as lastPing
            if (status == Status.HANDSHAKE && this.status == Status.OFFLINE)
                lastPing = System.currentTimeMillis();
            
            // if connection just finished
            if (status >= Status.ONLINE && this.status <= Status.HANDSHAKE)
                connectedAt = System.currentTimeMillis();
        }
        
        if (this.status != status) {
            APIManager.fireStatusChange(this, status, this.status);
            this.status = status;
            Tray.updateTray();
        }
    }
    
    public String getProfile_name () {
        return profile_name;
    }
    
    public void setProfile_name (String profile_name) {
        this.profile_name = profile_name;
    }
    
    public String getClient () {
        return client;
    }
    
    public void setClient (String text) {
        this.client = text;
    }
    
    public String getProfile_text () {
        return profile_text;
    }
    
    public void setProfile_text (String profile_text) {
        this.profile_text = profile_text;
    }
    
    public String getVersion () {
        return version;
    }
    
    public void setVersion (String text) {
        this.version = text;
    }
    
    public long getConnectTime () {
        return connectTime;
    }
    
    public void setConnectTime (long connectTime) {
        this.connectTime = connectTime;
    }
    
    public void setTheirCookie (String theirCookie) {
        this.theirCookie = theirCookie;
    }
    
    public static Boolean getBlack (String address) {
        return BuddyList.black.containsKey(address);
    }
    
    public static Boolean getHoly (String address) {
        return BuddyList.holy.containsKey(address);
    }
    
    public static boolean checkSock (Socket socket) {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    public static String getStatusName (byte b) {
        return StatusUtils.getStatusName(b);
    }
    
    public static Random getRandom () {
        return random;
    }
    
    public static void setRandom (Random random) {
        Buddy.random = random;
    }
    
    public String getAddress () {
        return address;
    }
    
    public void setAddress (String address) {
        this.address = address;
    }
    
    public String getName () {
        return name;
    }
    
    public void setName (String text) {
        this.name = text;
    }
    
    public boolean isFullyConnected () {
        return ourSockOut != null && ourSock != null && ourSock.isConnected() && !ourSock.isClosed() && theirSock != null && theirSock.isConnected() && !theirSock.isClosed();
    }
    
    public void setProfileName (String text) {
        this.profile_name = text;
    }
    
    public void setProfileText (String text) {
        this.profile_text = text;
    }
    
    public Socket getOurSock () {
        return ourSock;
    }
    
    public void setOurSock (Socket ourSock) {
        this.ourSock = ourSock;
    }
    
    public OutputStreamWriter getOurSockOut () {
        return ourSockOut;
    }
    
    public void setOurSockOut (OutputStreamWriter ourSockOut) {
        this.ourSockOut = ourSockOut;
    }
    
    public Socket getTheirSock () {
        return theirSock;
    }
    
    public void setTheirSock (Socket theirSock) {
        this.theirSock = theirSock;
    }
    
    public String getCookie () {
        return cookie;
    }
    
    public void setCookie (String cookie) {
        this.cookie = cookie;
    }
    
    public boolean isReceivedPong () {
        return receivedPong;
    }
    
    public void setReceivedPong (boolean receivedPong) {
        this.receivedPong = receivedPong;
    }
    
    public boolean isSentPong () {
        return sentPong;
    }
    
    public void setSentPong (boolean sentPong) {
        this.sentPong = sentPong;
    }
    
    public Object getConnectLock () {
        return connectLock;
    }
    
    public void setConnectLock (Object connectLock) {
        this.connectLock = connectLock;
    }
    
    public long getConnectedAt () {
        return connectedAt;
    }
    
    public void setConnectedAt (long connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public long getReconnectAt () {
        return reconnectAt;
    }
    
    public void setReconnectAt (long reconnectAt) {
        this.reconnectAt = reconnectAt;
    }
    
    public long getLastPing () {
        return lastPing;
    }
    
    public void setLastPing (long lastPing) {
        this.lastPing = lastPing;
    }
    
    public long getLastStatus () {
        return lastStatus;
    }
    
    public void setLastStatus (long lastStatus) {
        this.lastStatus = lastStatus;
    }
    
    public long getLastStatusRecieved () {
        return lastStatusRecieved;
    }
    
    public void setLastStatusRecieved (long lastStatusRecieved) {
        this.lastStatusRecieved = lastStatusRecieved;
    }
    
    public int getUnansweredPings () {
        return unansweredPings;
    }
    
    public void setUnansweredPings (int unansweredPings) {
        this.unansweredPings = unansweredPings;
    }
    
    public int getNpe1Count () {
        return npe1Count;
    }
    
    public void setNpe1Count (int npe1Count) {
        this.npe1Count = npe1Count;
    }
    
}
