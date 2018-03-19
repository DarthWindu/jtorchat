package core;
import gui.Gui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import util.ConfigWriter;
import util.RequestHTTP;




public class BuddyList {
	public static HashMap<String, Buddy> buds, black, holy;
	
	static {
	    buds = new HashMap<>();
	    black = new HashMap<>();
	    holy = new HashMap<>();
    }

	public static void  disconnect_all()
	{
		for (Buddy buddy : BuddyList.buds.values()) {
			if(buddy.isFullyConnected()) {
				try {
					buddy.sendDisconnect();
				} catch (IOException e) {
					//TODO Remove or do something in this empty catch block
				}
			}

			try {
				buddy.disconnect();
			} catch (IOException e) {
				//TODO Remove or do something in this empty catch block
			}
		}
	}

	public static void addBuddy(Buddy buddy) {
		BuddyList.buds.put(buddy.getAddress(), buddy);
		APIManager.fireNewBuddy(buddy);
	}

	public static void addBlack(Buddy buddy) {
		try {
			buddy.disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BuddyList.black.put(buddy.getAddress(), buddy);
		Gui.blacklist(buddy);
	}

	public static void addHoly(Buddy buddy) {
		BuddyList.holy.put(buddy.getAddress(), buddy);
		Gui.holylist(buddy);
	}

	public static void loadBuddies() throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileInputStream(Config.CONFIG_DIR + "bl.txt"));
		Random random = new Random();
		String line = "";
		Buddy buddy;

		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			// from 0 to 16 is address, 17 onwards is name
			if (line.length() > 15) {

				if (buds.containsKey(line.substring(0, 16))) 
					try {
						buds.remove(line.substring(0, 16)).disconnect();
					} catch (IOException e) {
						System.err.println("Error disconnecting buddy: " + e.getLocalizedMessage());
					}


				if(!line.substring(0, 16).equals(Config.us)) {
					if (line.length() > 16) {
						buddy = new Buddy(line.substring(0, 16), line.substring(17), true); 
						buddy.setReconnectAt(System.currentTimeMillis() + 15000 + random.nextInt(30000));
						ConfigWriter.loadbuddy(buddy);
					} else
						buddy = new Buddy(line.substring(0, 16), null, true);


					buddy.setReconnectAt(System.currentTimeMillis() + 15000 + random.nextInt(30000));
					ConfigWriter.loadbuddy(buddy);
				}
			}
		}

		loadBlack();
		loadHoly();
		scanner.close();
	}

	public static void loadBlack() throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileInputStream(Config.CONFIG_DIR + "blacklist.txt"));

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			// from 0 to 16 is address, 17 onwards is name
			if (line.length() > 15) {
				/*
				 * 
				 * 
				 * 
				 * IS THIS EMPTY BLOCK SUPPOSED TO BE HERE? 
				 * I feel like the following contents should be in this block instead.
				 *  	Sincerely,
				 *  		 Darth Windu
				 * 
				 * 
				 * 
				 * 
				 */
			}

			if (black.containsKey(line.substring(0, 16)))
				try {
					black.remove(line.substring(0, 16)).disconnect();
				} catch (IOException e) {
					System.err.println("Error disconnecting buddy: " + e.getLocalizedMessage());
				}


			if (buds.get(line.substring(0, 16)).getAddress().equals(line.substring(0, 16))) {
				BuddyList.black.put(line.substring(0, 16), buds.get(line.substring(0, 16)));
				Gui.blacklist(buds.get(line.substring(0, 16)));
			}
		}
		scanner.close();
	}

	public static void loadHoly() throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileInputStream(Config.CONFIG_DIR + "holylist.txt"));

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			// from 0 to 16 is address, 17 onwards is name
			if (line.length() > 15) {
				//Umm what? Sincerely, Darth Windu
			}
			if (holy.containsKey(line.substring(0, 16)))
				try {
					holy.remove(line.substring(0, 16)).disconnect();
				} catch (IOException e) {
					System.err.println("Error disconnecting buddy: " + e.getLocalizedMessage());
				}
			if (buds.get(line.substring(0, 16)).getAddress().equals(line.substring(0, 16)))
			{
				BuddyList.holy.put(line.substring(0, 16), buds.get(line.substring(0, 16)));
				Gui.holylist(buds.get(line.substring(0, 16)));
			}
		}
		scanner.close();
	}

	public static void saveBuddies() throws IOException {
		FileOutputStream fos = new FileOutputStream(Config.CONFIG_DIR + "bl.txt");
		for (Buddy buddy : buds.values())
		{
			ConfigWriter.savebuddy(buddy);

			if (buddy.getName() != null && buddy.getName().length() > 0) {
				fos.write((buddy.getAddress() + " " + buddy.getName() + "\n").getBytes());
			} else {
				fos.write((buddy.getAddress() + "\n").getBytes());
			}

		}
		fos.close();
		saveBlack();
		saveHoly();
	}

	public static void saveBlack() throws IOException {
		FileOutputStream fos = new FileOutputStream(Config.CONFIG_DIR + "blacklist.txt");
		for (Buddy buddy : black.values())
			fos.write((buddy.getAddress() + "\n").getBytes());
		fos.close();
	}

	public static void saveHoly() throws IOException {
		FileOutputStream fos = new FileOutputStream(Config.CONFIG_DIR + "holylist.txt");
		for (Buddy buddy : holy.values())
			fos.write((buddy.getAddress() + "\n").getBytes());
		fos.close();
	}


	/*
	 * Experimental listener for buddy updater Added
	 * "runStaticInit("buddyList");" to TCPort.java
	 */
	/*
	 * REMOTE BUDDY LOAD VIA TOR
	 */
	public static void loadBuddiesRemote(String remote_bl_URL) {

		Random random = new Random();
		ArrayList<String> input = RequestHTTP.load(remote_bl_URL);
		String line = "";
		String matchString = "^([a-zA-Z0-9]{16}(?:[ !].{0,}||))";

		for (int index = 0; index < input.size() && !line.startsWith("<CLOSE_STREAM>"); index++) {
			line = input.get(index);

			if (line.length() >= 16) {
				// regex checker
				if (line.matches(matchString)) {
					// from 0 to 16 is address, 17 onwards is name
					// Ignore any buddies already in your contact list
					if (!buds.containsKey(line.substring(0, 16))) {

						if(!line.substring(0, 16).equals(Config.us)){
							if (line.length() > 16) {
								Buddy b = new Buddy(line.substring(0, 16),
										line.substring(17),true); // .connect();
								b.setReconnectAt(System.currentTimeMillis()
														 + 15000 + random.nextInt(30000));
							} else {
								new Buddy(line.substring(0, 16), null, true).setReconnectAt(System
																									.currentTimeMillis()
																									+ 15000
																									+ random.nextInt(30000)); // .connect();
							}
						}
					}
				}
			}

      /*
       * Break statements are bad practice. Let's figure out a way to avoid them.
       * Perhaps adding !line.startsWith("<CLOSE_STREAM>") to the loop condition?
       *
       *  - Darth Windu
       */
			/*if(line.startsWith("<CLOSE_STREAM>")) {

				break;
				//FIXME Add condition to loop
			} */
		}   
	}

}
