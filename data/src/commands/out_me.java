package commands;

import gui.GuiChatWindow;
import util.ChatWindow;
import util.MessageType;
import core.Buddy;

public class out_me {
	public static void command(Buddy buddy, String s, GuiChatWindow window, boolean withDelay) {
	    ChatWindow.update_window(MessageType.RECEIVE_ACTION, window, s.substring(4),"","",withDelay);
	}
}