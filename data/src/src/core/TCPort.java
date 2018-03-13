package core;

import gui.Gui;
import gui.GuiLog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;


public class TCPort {

	// Direct compile and run from source for fixing the path
	public static String profile_name; 
	public static String profile_text;
	public static String status = "available"; // available away xa
	public static String[] extern_source_path;
	private static boolean launched;
	@SuppressWarnings("unused")
	private static boolean halted;



	public static void main(String[] args) {
		GuiLog.instance = new GuiLog();
		extern_source_path=args;

		try {


			if (GuiLog.instance != null)
			{
				if(Config.visiblelog == 1) {
					GuiLog.instance.setVisible(true);
				}
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					Logger.setOverride(true);
					Logger.stopGLog();
					Logger.log(Logger.INFO, "Shutdown", "Starting...");
					// for (Buddy b : BuddyList.buds.values()) {
					// try {
					// b.disconnect();
					// } catch (IOException e) {
					// System.err.println("Error disconnecting " + b.getAddress() + ": " + e.getLocalizedMessage());
					// }
					// }
					TorLoader.cleanUp();
					try {
						if (launched)
							BuddyList.saveBuddies();
						BuddyList.disconnect_all();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});




			if (Config.loadTor == 1)
				TorLoader.loadTor();


			TCServ.init();

			new Gui().init();

			launched = true;
			try {
				BuddyList.loadBuddies();
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
				// no buddylist file
			}

			if (Config.us == null || Config.us.length() != 16) {
				Logger.log(Logger.FATAL, "TCPort", "Config.us: " + Config.us + " is invalid.");
				JTextField jtf = new JTextField();
				jtf.setEditable(false);
				jtf.setText("Config.us: " + Config.us + " is invalid.");
				JOptionPane.showMessageDialog(null, jtf, "Fatal Error", JOptionPane.PLAIN_MESSAGE);	
				// System.exit(-1);
			}


			new fileTransfer.FileTransfer();


			ThreadManager.registerWork(ThreadManager.DAEMON, new Runnable() {

				@Override
				public void run() {
					try {
						Scanner s = new Scanner(System.in);
						try {
							while (s.hasNextLine()) {
								String l = s.nextLine();
								if (l.startsWith("tskill ")) { // kills theirsock of the buddy following tskill
									BuddyList.buds.get(l.split(" ")[1]).theirSock.close();
									BuddyList.buds.get(l.split(" ")[1]).theirSock = null;
								} else if (l.startsWith("oskill ")) { // kills oursock of the buddy following oskill
									BuddyList.buds.get(l.split(" ")[1]).ourSock.close();
									BuddyList.buds.get(l.split(" ")[1]).ourSock = null;
								} else if (l.startsWith("msg ")) { // send messaage to a buddy
									BuddyList.buds.get(l.split(" ")[1]).sendMessage(l.split(" ", 3)[2]);
								} else if (l.startsWith("raw ")) { // send raw messaage to a buddy
									BuddyList.buds.get(l.split(" ")[1]).sendRaw(l.split(" ", 3)[2]);

								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						s.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}


			}, "Starting console.", "Console thread");

			ThreadManager.registerWork(ThreadManager.DAEMON, new Runnable() {

				@Override
				public void run() {
					while (true) {
						try {

							if (Config.nowstart != "") {
								BuddyList.loadBuddiesRemote(Config.nowstart);
								Config.nowstart = "";
							}

							if (Config.nowstartupdate != "") {
								Config.LastCheck = Update.loadUpdate(Config.nowstartupdate);

								if (Config.LastCheck != "close") {
									JTextField jtf = new JTextField();
									jtf.setEditable(false);
									jtf.setText(Config.LastCheck);
									JOptionPane.showMessageDialog(null, jtf, "Update Check", JOptionPane.PLAIN_MESSAGE);
								}


								Config.nowstartupdate = "";
							}

							for (Buddy buddy : BuddyList.buds.values()) {
								if (buddy.getConnectTime() != -1 && System.currentTimeMillis() - buddy.getConnectTime() > Config.CONNECT_TIMEOUT * 1000) {
									// checks if buddy hasnt finished connecting within CONNECT_TIMEOUT seconds
									// if it hasnt then reset
									if (buddy.ourSock != null)
										buddy.ourSock.close();
									if (buddy.theirSock != null)
										buddy.theirSock.close();
									buddy.ourSock = null;
									buddy.theirSock = null;
									buddy.setStatus(Buddy.Status.OFFLINE);
									buddy.connect();
									Logger.log(Logger.INFO, "Status Thread", "Connection reset for " + buddy.getAddress());
								}

								if (buddy.getStatus() >= Buddy.Status.ONLINE && (buddy.ourSock == null || buddy.theirSock == null || buddy.ourSock.isClosed() || buddy.theirSock.isClosed())) {
									if (buddy.ourSock != null)
										buddy.ourSock.close();

									buddy.ourSock = null;
									if (buddy.theirSock != null)
										buddy.theirSock.close();

									buddy.theirSock = null;
									buddy.setStatus(Buddy.Status.OFFLINE);
								} else if (buddy.getStatus() == Buddy.Status.HANDSHAKE && (buddy.ourSock == null || buddy.ourSock.isClosed())) {
									if (buddy.ourSock != null)
										buddy.ourSock.close();

									buddy.ourSock = null;
									buddy.setStatus(Buddy.Status.OFFLINE);
								}
								// TODO check unsanswered pings
								if (buddy.unansweredPings > 5)
									buddy.disconnect();

								if (buddy.ourSock != null && buddy.ourSockOut != null && buddy.theirSock != null && buddy.getStatus() >= Buddy.Status.ONLINE && System.currentTimeMillis() - buddy.lastStatusRecieved > Config.DEAD_CONNECTION_TIMEOUT * 1000) {
									Logger.log(Logger.INFO, "Status Thread", "");
									buddy.disconnect();
								}
								if (buddy.ourSock != null && buddy.theirSock != null && buddy.recievedPong && buddy.getTimeSinceLastStatus() > (Config.KEEPALIVE_INTERVAL - 20) * 1000)
									buddy.sendStatus(); // Sends status every 100 seconds supposed to be every
								// KEEPALIVE_INTERVAL but 20 seconds shorter just incase :\
								if (buddy.ourSock != null && buddy.ourSockOut != null && !buddy.recievedPong && (System.currentTimeMillis() - buddy.lastPing) > (Config.KEEPALIVE_INTERVAL / 4) * 1000)
									buddy.sendPing(); 

								if (buddy.reconnectAt != -1 && System.currentTimeMillis() - buddy.reconnectAt > 0) {
									// Retries connection after having waited it out
									Logger.log(Logger.INFO, "Status Thread", "Retrying connection to " + buddy.getAddress() + " as it is past " + buddy.reconnectAt);
									buddy.connect();
								}
							}
							Thread.sleep(5000);
							// System.out.println("Status Ping");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}, "Starting status thread.", "Status thread");


			Logger.log(Logger.INFO, "Init", "Done.");
			Logger.setOverride(false);
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				// ignored
				//TODO Do something with this ignored catch block :D
			}

		} catch (Exception e) {
			halt(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void halt(Exception e) {
		halted = true;
		System.err.println("*** Error during startup, Halting! ***");
		e.printStackTrace();
		TCServ.halt();
		if (GuiLog.instance != null) {
			GuiLog.instance.setVisible(true);
			GuiLog.instance.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			if (launched) {
				try {
					BuddyList.saveBuddies();
				} catch (IOException e1) {
					Logger.log(Logger.WARNING, "The End", "Error saving buddies: " + e1.getLocalizedMessage());
				}
				for (Buddy b : ((HashMap<String, Buddy>) BuddyList.buds.clone()).values())
					try {
						b.remove();
					} catch (IOException e1) {
						Logger.log(Logger.WARNING, "The End", "Error removing buddy " + (b == null ? "null" : b.toString(true)) + ": " + e1.getLocalizedMessage());
					}
				launched = false;
			}
			try {
				Class<?> c = Class.forName("gui.Gui");
				Object i = c.getDeclaredField("instance").get(null);
				Field f = c.getDeclaredField("f");
				f.setAccessible(true);
				((JFrame) f.get(i)).dispose();
			} catch (Exception ee) {
				ee.printStackTrace(); // should be ignored
			}
		}
		System.err.println("*** Error during startup, Halted! ***");
	}


	public static void sendMyInfo() {
		for (Buddy buddy : BuddyList.buds.values()) {
			if (buddy.getStatus() >= Buddy.Status.ONLINE) {
				try {
					buddy.sendClient();
					buddy.sendVersion();
					buddy.sendProfileName();
					buddy.sendProfileText();
					buddy.sendStatus();
				} catch (IOException ioe) {
					try {
						ioe.printStackTrace();
						buddy.disconnect(); // something is iffy if we error out
					} catch (IOException e) {
						// ignored
					}
				}
			}
		}
	}

	public static void sendMyProfil() /*  There's a typo in the method name (I think)!  */{
		for (Buddy buddy : BuddyList.buds.values()) {
			if (buddy.getStatus() >= Buddy.Status.ONLINE) {
				try {
					buddy.sendProfileName();
					buddy.sendProfileText();
				} catch (IOException ioe) {
					try {
						ioe.printStackTrace();
						buddy.disconnect(); // something is iffy if we error out
					} catch (IOException e) {
						// ignored
						//plzzz this is badd practice - DW
					}
				}
			}
		}
	}

	public static void sendMyStatus() {
		for (Buddy buddy : BuddyList.buds.values()) {
			if (buddy.getStatus() >= Buddy.Status.ONLINE) {
				try {
					buddy.sendStatus();
				} catch (IOException ioe) {
					try {
						ioe.printStackTrace();
						buddy.disconnect(); // something is iffy if we error out
					} catch (IOException e) {
						// ignored
						//plzzz this is badd practice - DW
					}
				}
			}
		}
	}

}
