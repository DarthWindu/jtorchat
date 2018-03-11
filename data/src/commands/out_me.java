package commands;

import gui.GuiChatWindow;
import util.ChatWindow;
import util.MessageType;
import core.Buddy;

public class out_me {
    public static void command(Buddy buddy, String command, GuiChatWindow window, boolean withDelay) {
        ChatWindow.update_window(MessageType.RECEIVE_ACTION, window, command.substring(4), "", "", withDelay);
    }
}