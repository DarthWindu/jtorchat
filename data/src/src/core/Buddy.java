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
import util.Tray;




public class Buddy {

	public static final byte OFFLINE = 0;
	public static final byte HANDSHAKE = 1;
	public static final byte ONLINE = 2;
	public static final byte AWAY = 3;
	public static final byte XA = 4;

	String address;
	private String name; // wtf is this for?, custom name? todo use this somewhere -- used
	public volatile Socket ourSock = null; // Our socket to them - the output sock
	public volatile OutputStreamWriter ourSockOut = null;
	public volatile Socket theirSock = null; // Their socket to us - the input sock
	String cookie = null;
	private String theirCookie = null;
	byte status = OFFLINE;

	private int connectFailCount = 0;

	public static Random random = new Random();
	public boolean recievedPong;
	private boolean sentPong;
	String profile_name;
	String client = "";

	String version = "";
	String profile_text;
	private Object connectLock = new Object();

	private long connectedAt;
	private long connectTime;
	protected long reconnectAt;
	public long lastPing = -1;
	public long lastStatus = -1;
	public long lastStatusRecieved;
	public int unansweredPings;

	private int npe1Count;

	public Buddy(String address, String name, Boolean now) {

		this.address = address;
		this.name = name;

		this.cookie = generateCookie();

		if (now) // Prevent flooding
		{
			BuddyList.addBuddy(this); 
		}
	}

	public static String generateCookie() {
		String cookie = "";
		String alphaNumeric = "abcdefghijklmnopqrstuvwxyz1234567890";
		for (int i = 0; i < 77; i++)
			cookie += alphaNumeric.charAt(random.nextInt(alphaNumeric.length()));
		return cookie;
	}

	public void connect() {
		if (!getBlack(this.address))
		{	
			if (ourSock != null) {
				reconnectAt = -1;
				Logger.log(Logger.WARNING, this, "Connect(V)V was called but ourSock isn't null!");
				Thread.dumpStack();
				return;
			}
			// maybe store sock connection in another variable then move it to
			// ourSock when connected - fixed with ourSockOut
			ThreadManager.registerWork(ThreadManager.NORMAL, () -> {
                if (!getBlack(Buddy.this.address))
                {
                    try {
                        reconnectAt = -1;
                        connectTime = System.currentTimeMillis();
                        ourSock = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", Config.SOCKS_PORT)));
                        ourSock.connect(InetSocketAddress.createUnresolved(address + ".onion", 11009));
                        setStatus(HANDSHAKE);
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
                        setStatus(OFFLINE);
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

                            Logger.log(Logger.SEVERE, Buddy.this.getClass(),"BROKEN - " + address);
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


	public void setmyStatus(int status) {

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



	protected void setStatus(byte status) {
		if (status == OFFLINE) {
			// if (address.equals("jutujsy2ufg33ckl"))
			// Thread.dumpStack();
			reconnectAt = connectFailCount < 4 ? (15) : connectFailCount < 15 ? (5 * 60) : (30 * 60);
			reconnectAt *= (7 + random.nextInt(13)) / 10d;
			reconnectAt *= 1000;
			reconnectAt += System.currentTimeMillis();
			sentPong = false;
			recievedPong = false;
			unansweredPings = 0;
			ourSockOut = null;

			if (this.status >= Buddy.ONLINE) {
                Logger.log(Logger.INFO, this, address + " connected for " + (connectedAt == -1 ? " Not Set" : ((System.currentTimeMillis() - connectedAt) / 1000)));
                connectedAt = -1;
            }
		}

		//NOTE A: THE DIFFERENCE BETWEEN status and this.status in the if conditions
        noteA: {
            // During handshake lastStatus is used as lastPing
            if (status == HANDSHAKE && this.status == OFFLINE)
                lastPing = System.currentTimeMillis();

            // if connection just finished
            if (status >= ONLINE && this.status <= HANDSHAKE)
                connectedAt = System.currentTimeMillis();
        }

        if (this.status != status) {
			APIManager.fireStatusChange(this, status, this.status);
			this.status = status;
			Tray.updateTray();
		}
	}

	public void sendPing() throws IOException {
	    String command = "ping";
		sendRaw(command + " " + Config.us + " " + cookie);//IOException can occur here
		unansweredPings++;//Umm, always? Maybe I'm misunderstanding
		if (status == OFFLINE || status == HANDSHAKE)
			lastPing = System.currentTimeMillis();
	}



	public void sendRaw(String command) throws IOException {
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

	public String getAddress() {
		return address;
	}

	public static Boolean getBlack(String address) {
        return BuddyList.black.containsKey(address);
	}

	public static Boolean getHoly(String address) {
        return BuddyList.holy.containsKey(address);
	}

	public void sendPong(String pong) throws IOException {
		sendRaw("pong " + pong);
		sentPong = true;
	}

	public void sendClient() throws IOException {
		sendRaw("client " + Config.CLIENT);
	}

	public void sendVersion() throws IOException {
		sendRaw("version " + Config.VERSION);
	}

	public void sendProfileName() throws IOException {
		sendRaw("profile_name " + TCPort.profile_name);
	}

	public void sendProfileText() throws IOException {
		sendRaw("profile_text " + TCPort.profile_text);
	}

	public void sendAddMe() throws IOException {
		sendRaw("add_me");
	}

	public void sendStatus() throws IOException {

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

	public void setTheirCookie(String theirCookie) {
		this.theirCookie = theirCookie;
	}

	public void onFullyConnected() throws IOException {
		sendClient();
		sendVersion();
		sendProfileName();
		sendProfileText();
        sendAddMe();
		sendStatus();


		if (!BuddyList.buds.containsKey(this.address))
		    BuddyList.addBuddy(this);

	}

	public void attatch(Socket socket, Scanner sc) throws IOException {
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
			if (status == OFFLINE && connectTime != -1) {
				// connect() method is trying to connect atm

				// is severe so its printed on err
                Logger.log(Logger.SEVERE, this, "status == OFFLINE && connectTime != -1");
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
			this.recievedPong = false;
			try {
				while (sc.hasNext()) {
					if (!getBlack(this.address))
					{
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
				Logger.log(Logger.DEBUG, this, "[" + address + "] attatch() " + se.getLocalizedMessage() + " | " + se.getStackTrace()[0]);
				// SocketExceptions are quite common and generally nothing to worry about
			} catch (IOException e) {
				Logger.log(Logger.WARNING, this, "[" + address + "] theirSock = null; ourSock = null; " + e.getLocalizedMessage() + " | " + e.getStackTrace()[0]);
				disconnect();
				throw e;
			}
		}
	}

	public static boolean checkSock(Socket socket) {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	public void sendMessage(String string) throws IOException {
		sendRaw("message " + string);
	}

	public void sendDisconnect() throws IOException {
		sendRaw("disconnect");
	}

	public static String getStatusName(byte b) {
		return b == OFFLINE ? "Offline" : b == HANDSHAKE ? "Handshake" : b == ONLINE ? "Online" : b == AWAY ? "Away" : b == XA ? "Extended Away" : "Idk.";
	}

	public long getTimeSinceLastStatus() {
		return System.currentTimeMillis() - lastStatus;
	}

	@Override
	public String toString() {
		return (name != null && name.length() > 0) ? name : (profile_name != null && profile_name.length() > 0) ? profile_name : "[" + address + "]"; // + " (" + address + ")";
	}


	public byte getStatus() {
		return status;
	}

	public String getProfile_name() {
		return profile_name;
	}

	public String getClient() {
		return client;
	}

	public String getProfile_text() {
		return profile_text;
	}

	public String getVersion() {
		return version;
	}

	public long getConnectTime() {
		return connectTime;
	}

	public void disconnect() throws IOException { // should be used with caution
		if (ourSock != null)
			ourSock.close();
		ourSock = null;
		if (theirSock != null)
			theirSock.close();
		theirSock = null;
		setStatus(OFFLINE);
		Logger.log(Logger.NOTICE, this, "Disconnect called on " + address + " | Retry in " + (reconnectAt - System.currentTimeMillis()));
	}

	public void remove() throws IOException {
		ConfigWriter.deletebuddy(this);
		BuddyList.buds.remove(this.address);
		try {
			if(this.isFullyConnected()) {
				this.sendRaw("remove_me");
			} 
		} catch (IOException e) {
			//TODO REMOVE OR DO SOMETHING IN THIS EMPTY CATCH BLOCK
		}
		disconnect();
		APIManager.fireBuddyRemoved(this);
	}

	public String getName() {
		return name;
	}

	public String toString(boolean b) {
		return address.equals(Config.us) ? language.langtext[61] : (profile_name != null && profile_name.length() > 0) ? profile_name + " (" + address + ")" : (name != null && name.length() > 0) ? name + " (" + address + ")" : "[" + address + "]"; // + " (" + address + ")";
	}

	public boolean isFullyConnected() {
		return ourSockOut != null && ourSock != null && ourSock.isConnected() && !ourSock.isClosed() && theirSock != null && theirSock.isConnected() && !theirSock.isClosed();
	}

	public void setName(String text) {
		this.name = text;
	}
	public void setProfileName(String text) {
		this.profile_name = text;
	}
	public void setProfileText(String text) {
		this.profile_text = text;
	}
	public void setVersion(String text) {
		this.version = text;
	}
	public void setClient(String text) {
		this.client = text;
	}

}
