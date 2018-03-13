package core;

import java.io.IOException;
import java.io.InputStream;

import fileTransfer.FileTransfer;
import util.Status;

public class BuddyIncoming {
    private static String charsToReplace = "[^a-zA-Z_]";
    
	public static void init(String in, Buddy buddy)	{
		//TODO determine if FixMe A is still relevant
		// FIXME FixMe A: Fix filedata Problems and make it saver
		String save = in.split(" ")[0].replaceAll(charsToReplace, "");
		initSwitchSave(save, in, buddy);
	}


	// Why there are two incoming streams?
	public static void init_outin(String input, Buddy buddy, InputStream inputStream) {
		// Fix filedata Problems and make it saver
		String save = input.split(" ")[0].replaceAll(charsToReplace, "");

		if (save.equals("filename")) {FileTransfer.in_filename(buddy,input,inputStream);}
		else if (save.equals("filedata")) {FileTransfer.in_filedata(buddy,input,inputStream);}

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
		buddy.setLastStatusRecieved(System.currentTimeMillis());
		byte status = in.split(" ")[1].equalsIgnoreCase("available") ? Status.ONLINE : in.split(" ")[1].equalsIgnoreCase("xa") ? Status.XA : in.split(" ")[1].equalsIgnoreCase("away") ? Status.AWAY : -1;
		buddy.setStatus(status); // checks for change in method
	}


	private static void in_profile_name(String in, Buddy buddy) {
		String old = buddy.getProfile_name();
		buddy.setProfile_name(in.split(" ", 2)[1]);
		APIManager.fireProfileNameChange(buddy, buddy.getProfile_name(), old);
	}


	private static void in_client(String in, Buddy buddy) {
		buddy.setClient(in.split(" ", 2)[1]);
	}


	private static void in_version(String in, Buddy buddy) {
		buddy.setVersion(in.split(" ", 2)[1]);
	}


	private static void in_profile_text(String in, Buddy buddy) {
		String old = buddy.getProfile_text();
		buddy.setProfile_text(in.split(" ", 2)[1]);
		APIManager.fireProfileTextChange(buddy, buddy.getProfile_text(), old);
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
		Logger.log(Logger.NOTICE, buddy, "Recieved " + in.trim() + " from " + buddy.getAddress());
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
		Logger.log(Logger.WARNING, buddy, "Recieved unknown from " + buddy.getAddress() + " " + in);
		try {
			buddy.sendRaw("not_implemented ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static void in_pong(String in, Buddy buddy) {
		if (in.split(" ")[1].equals(buddy.getCookie())) {
			buddy.setUnansweredPings(0);
			buddy.setReceivedPong(true);
			Logger.log(Logger.NOTICE, buddy, buddy.getAddress() + " sent pong");
			if (buddy.getOurSock() != null && buddy.getOurSockOut() != null && buddy.getStatus() > Status.OFFLINE) {
				try {
					buddy.onFullyConnected();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				Logger.log(Logger.SEVERE, buddy, "[" + buddy.getAddress() + "] - :/ We should be connected here. Resetting connection!");
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
			Logger.log(Logger.SEVERE, buddy, "!!!!!!!!!! " + buddy.getAddress() + " !!!!!!!!!! sent us bad pong !!!!!!!!!!");
			Logger.log(Logger.SEVERE, buddy, "!!!!!!!!!! " + buddy.getAddress() + " !!!!!!!!!! ~ Disconnecting them");
			try {
				buddy.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private static void in_ping(String in, Buddy buddy) {
		if (buddy.getOurSock() == null) {
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
