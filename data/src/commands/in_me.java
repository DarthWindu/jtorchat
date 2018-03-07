package commands;

import gui.GuiChatWindow;
import util.ChatWindow;
import util.MessageType;
import core.Buddy;

public class in_me {
	public static void command(Buddy buddy, String str, GuiChatWindow window) {

		if (str.length() < 5)
			ChatWindow.update_window(MessageType.PRIVATE, window,"Parameter /me msg","","",false);
		else
			ChatWindow.update_window(MessageType.SEND_ACTION, window,str.substring(4),"","/me " + str.substring(4),!buddy.isFullyConnected());
	}
}
