package core;

import java.io.IOException;
import java.io.InputStream;

import fileTransfer.FileTransfer;

public class BuddyIncoming {
	public static void init(String in, Buddy buddy)	{
		// Fix filedata Problems and make it saver
		String save = in.split(" ")[0].replaceAll("[^a-zA-Z_]", "");
		initSwitchSave(save, in, buddy);
	}


	// Why there are two incoming streams?
	public static void init_outin(String in, Buddy b, InputStream c) {
		// Fix filedata Problems and make it saver
		String save = in.split(" ")[0].replaceAll("[^a-zA-Z_]", "");

		if (save.equals("filename")) {FileTransfer.in_filename(b,in,c);}
		else if (save.equals("filedata")) {FileTransfer.in_filedata(b,in,c);}

	}


	private static void initSwitchSave(String save, String in, Buddy buddy) {
		switch (save) {
		case "status":
			in_status(in, buddy);
			break;
		case "ping":
			in_ping(in,buddy);
			break;
		case "pong":
			in_pong(in,buddy);
			break;
		case "profile_name":
			in_profile_name(in,buddy);
			break;
		case "client":
			in_client(in,buddy);
			break;
		case "version":
			in_version(in,buddy);
			break;
		case "profile_text":
			in_profile_text(in,buddy);
			break;
		case "add_me":
			in_add_me(in,buddy);
			break;
		case "remove_me":
			in_remove_me(in,buddy);
			break;
		case "message":
			in_message(in,buddy);
			break;
		case "disconnect":
			in_disconnect(in,buddy);
			break;
		case "not_implemented":
			in_not_implemented(in,buddy);
			break;
		case "profile_avatar":
			in_profile_avatar(in,buddy);
			break;
		case "filedata_ok":
			FileTransfer.in_filedata_ok(buddy,in);
			break;
		case "filedata_error":
			FileTransfer.in_filedata_error(buddy,in);
			break;
		case "file_stop_sending":
			FileTransfer.in_file_stop_sending(buddy,in);
			break;
		case "file_stop_receiving":
			FileTransfer.in_file_stop_receiving(buddy,in);
			break;
		default:
			in_nothing(in, buddy);
			break;
		}
	}


	private static void in_status(String in, Buddy buddy) {
		buddy.lastStatusRecieved = System.currentTimeMillis();
		byte nstatus = in.split(" ")[1].equalsIgnoreCase("available") ? Buddy.ONLINE : in.split(" ")[1].equalsIgnoreCase("xa") ? Buddy.XA : in.split(" ")[1].equalsIgnoreCase("away") ? Buddy.AWAY : -1;
		buddy.setStatus(nstatus); // checks for change in method
	}


	private static void in_profile_name(String in, Buddy buddy) {
		String old = buddy.profile_name;
		buddy.profile_name = in.split(" ", 2)[1];
		APIManager.fireProfileNameChange(buddy, buddy.profile_name, old);
	}


	private static void in_client(String in, Buddy buddy) {
		buddy.client = in.split(" ", 2)[1];
	}


	private static void in_version(String in, Buddy buddy) {
		buddy.version = in.split(" ", 2)[1];
	}


	private static void in_profile_text(String in, Buddy buddy) {
		String old = buddy.profile_text;
		buddy.profile_text = in.split(" ", 2)[1];
		APIManager.fireProfileTextChange(buddy, buddy.profile_text, old);
	}


	private static void in_add_me(String in, Buddy buddy) {
		APIManager.fireAddMe(buddy);
	}


	private static void in_remove_me(String in, Buddy buddy) {
		APIManager.fireRemove(buddy);
	}


	private static void in_message(String in, Buddy buddy) {
		APIManager.fireMessage(buddy, in.split(" ", 2)[1]);
	}


	private static void in_not_implemented(String in, Buddy buddy) {
		Logger.log(Logger.NOTICE, buddy, "Recieved " + in.trim() + " from " + buddy.address);
	}


	private static void in_profile_avatar(String in, Buddy buddy) {
		Logger.log(Logger.NOTICE, buddy, "Sorry, we have no avatar support. Coming soon.");
	}


	private static void in_disconnect(String in, Buddy buddy) {
		Logger.log(Logger.NOTICE, buddy, "Recieved disconnect command from " + buddy.getAddress());
		try {
			buddy.disconnect();
		} catch (IOException e) {
			//TODO Remove or do something in this empty catch block
		}
	}


	private static void in_nothing(String in, Buddy buddy) {
		Logger.log(Logger.WARNING, buddy, "Recieved unknown from " + buddy.address + " " + in);
		try {
			buddy.sendRaw("not_implemented ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static void in_pong(String in, Buddy buddy) {
		if (in.split(" ")[1].equals(buddy.cookie)) {
			buddy.unansweredPings = 0;
			buddy.recievedPong = true;
			Logger.log(Logger.NOTICE, buddy, buddy.address + " sent pong");
			if (buddy.ourSock != null && buddy.ourSockOut != null && buddy.status > Buddy.OFFLINE) {
				try {
					buddy.onFullyConnected();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				Logger.log(Logger.SEVERE, buddy, "[" + buddy.address + "] - :/ We should be connected here. Resetting connection!");
				try {
					buddy.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				buddy.connect();
				return;
			}
		} else {
			Logger.log(Logger.SEVERE, buddy, "!!!!!!!!!! " + buddy.address + " !!!!!!!!!! sent us bad pong !!!!!!!!!!");
			Logger.log(Logger.SEVERE, buddy, "!!!!!!!!!! " + buddy.address + " !!!!!!!!!! ~ Disconnecting them");
			try {
				buddy.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private static void in_ping(String in, Buddy buddy) {
		if (buddy.ourSock == null) {
			buddy.connect();
		}
		try {
			try {
				buddy.sendPong(in.split(" ")[2]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NullPointerException npe) {
			try {
				buddy.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
