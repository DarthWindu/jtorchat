package core;

import gui.GuiKillTor;
import gui.GuiLog;
import gui.GuiTorLoading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

import util.ConfigWriter;



public class TorLoader {
	private static final String CLASS_NAME = TorLoader.class.getName();
	private static Process process;
	private static Object loadLock = new Object();

	public static void loadTor() {
		final GuiTorLoading torLoading = new GuiTorLoading();
		torLoading.getProgressBar1().setIndeterminate(true);
		torLoading.setVisible(true);
		ThreadManager.registerWork(ThreadManager.DAEMON, new Runnable() {

			@Override
			public void run() {

				if (Config.answer!=null)  {
					// if a language file NOT found
					Logger.log(Logger.FATAL, CLASS_NAME, Config.answer);
					GuiLog.instance.setVisible(true);
					torLoading.getProgressBar1().setValue(0);
					torLoading.getProgressBar1().setIndeterminate(false);
					torLoading.gettextArea1().setText(Config.answer);
				} else {

					if (Config.offlineMod == 0) {
						Boolean ssfail = false;
						try {
							ServerSocket ss = new ServerSocket(Config.LOCAL_PORT);
							ss.close();
						} catch (IOException e) {
							ssfail = true;
						}

						if (ssfail) {
							Logger.log(Logger.FATAL, "TCServ", "Failed to test local server.");
							torLoading.getProgressBar1().setValue(0);
							torLoading.gettextArea1().setText("Can not bind a port for jtorchat, is another jtorchat instance activ?");
							torLoading.getProgressBar1().setIndeterminate(false);
						} else {

							if (Config.loadTor == 1)  {
								// only load portable tor if not testing
								ProcessBuilder procBuilder = instantiateProcessBuilder();

								procBuilder.directory(new File(Config.TOR_DIR).getAbsoluteFile());
								System.out.println(new File(Config.TOR_DIR).getAbsolutePath());
								procBuilder.redirectErrorStream(true);

								try {
									process = procBuilder.start();
									Scanner sc = new Scanner(process.getInputStream());
									while (sc.hasNextLine()) {
										String line = sc.nextLine();
										Logger.log(Logger.INFO, "Tor", line);

										// New progress function (obfsproxy has more then three progress messages)
										if (line.contains("Bootstrapped ")) {
											String[] starting = line.split("%");
											if (starting.length == 2) {
												starting = starting[0].split(" ");
												if (starting.length == 6) {
													int starting3 = Integer.parseInt(starting[5]);
													if (starting3 <= 100 || starting3 > 0) {
														torLoading.getProgressBar1().setValue(Integer.parseInt(starting[5]));
														torLoading.gettextArea1().setText(language.langtext[51]);
														torLoading.getProgressBar1().setIndeterminate(false);
														if(starting3 == 100) {
															synchronized(loadLock) {
																Config.us = new Scanner(new FileInputStream(Config.TOR_DIR + "hidden_service/hostname")).nextLine().replace(".onion", "");
																Logger.log(Logger.NOTICE, CLASS_NAME, "Set 'us' to " + Config.us);
																loadLock.notifyAll();
																torLoading.dispose();
															}
														}
													}
												}
											}
										}


										if (line.contains("Failed to bind one of the listener")) {
											Logger.log(Logger.FATAL, CLASS_NAME, "The listener Port is in use.");

											if (GuiKillTor.newpb == 0) {
												torLoading.gettextArea1().setText("Wait for a answer!");
												GuiKillTor.listenerport();
												process.destroy();
												procBuilder = instantiateProcessBuilder();
												procBuilder.directory(new File(Config.TOR_DIR).getAbsoluteFile());
												System.out.println(new File(Config.TOR_DIR).getAbsolutePath());
												procBuilder.redirectErrorStream(true);
												process = procBuilder.start();
												sc = new Scanner(process.getInputStream());
												GuiKillTor.newpb = 2;
											} else {
												torLoading.getProgressBar1().setValue(0);
												torLoading.getProgressBar1().setIndeterminate(false);
												torLoading.gettextArea1().setText(language.langtext[48]);
											}
										}

										if (line.contains("Network is unreachable")) {
											Logger.log(Logger.FATAL, CLASS_NAME, "Network is unreachable.");
											torLoading.getProgressBar1().setValue(0);
											torLoading.getProgressBar1().setIndeterminate(false);
											torLoading.gettextArea1().setText("Network is unreachable.");
										}


										if (line.contains("broken state. Dying.")) {
											Logger.log(Logger.FATAL, CLASS_NAME, "Tor has died apparently, Ja.");
											GuiLog.instance.setVisible(true);
											torLoading.getProgressBar1().setValue(0);
											torLoading.getProgressBar1().setIndeterminate(false);
											torLoading.gettextArea1().setText(language.langtext[49]);
										}
									}
									sc.close();
								} catch (IOException e) {
									Logger.log(Logger.SEVERE, CLASS_NAME, e.getLocalizedMessage());
									if (e.getLocalizedMessage().contains("Cannot")) {
										GuiLog.instance.setVisible(true);
										torLoading.getProgressBar1().setValue(0);
										torLoading.getProgressBar1().setIndeterminate(false);
										torLoading.gettextArea1().setText(language.langtext[50]);
									}
								}
							}
						}

					} else {
						torLoading.dispose();
					}
				}
			}
		}, "Starting Tor.", "Tor Monitor Thread");

		if (Config.offlineMod == 0 & Config.loadTor == 1) {
			try {
				synchronized(loadLock) {
					loadLock.wait();
				}
			} catch (InterruptedException e) {
				//TODO remove or do something
			}
		}
	}
	
	

	public static void cleanUp() {
		synchronized(loadLock) {
			loadLock.notifyAll();
		}

		Config.firststart = 0;
		ConfigWriter.saveall(2);


		if (process != null) {
			Logger.log(Logger.INFO, CLASS_NAME, "Cleaning up.");
			process.destroy();
		}



		// fix: On Windows obfsproxy do not close when his linked tor close --> make better in future
		if (Config.os.contains("win") && Config.obfsproxy==1) {
			String command1="taskkill /F /IM jobfsproxy.exe";
			Runtime run = Runtime.getRuntime();
			try {
				Process pr1 = run.exec(command1);
				try {
					pr1.waitFor();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	

	private static ProcessBuilder instantiateProcessBuilder() {
		ProcessBuilder pb;
		Logger.log(Logger.NOTICE, "TorLoader ", "Checking OS");
		// PROCESSBUILDER settings for WINDOWS
		if (Config.os.contains("win")) {

			// if so, then start tor.exe with torrc.txt
			Logger.log(Logger.NOTICE, CLASS_NAME, "Start portable tor in windows");
			pb = new ProcessBuilder(Config.TOR_DIR + Config.Torbinary, "-f", Config.TOR_DIR + Config.Tortorrc);

		} else if (Config.os.contains("nix") || Config.os.contains("nux")) {

			// PROCESSBUILDER settings for LINUX and UNIX
			Logger.log(Logger.NOTICE, CLASS_NAME, "Start portable tor in *nix or linux");

			pb = new ProcessBuilder(Config.TOR_DIR +Config.Torbinary,"-f",Config.TOR_DIR + Config.Tortorrc);
			pb.environment().put("LD_LIBRARY_PATH", Config.TOR_DIR+Config.TorLinlib);
			pb.environment().put("LDPATH", Config.TOR_DIR+"linux/lib/");

		} else {
			Logger.log(Logger.NOTICE, CLASS_NAME,"Can't detect OS type, using system tor (need system tor to work)");
			pb = new ProcessBuilder("tor", "-f", Config.TOR_DIR + Config.Tortorrc );
		}
		
		return pb;
	}

}

