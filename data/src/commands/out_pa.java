package commands;

import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;
import core.Buddy;

public class out_pa {
	public static void command(Buddy buddy, String s, GuiChatWindow window, boolean withDelay) {
	    ChatWindow.update_window(MessageType.RECEIVE_PAGE, window,s.substring(4),"","",withDelay);
	}
}
