package commands;

import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;
import core.Buddy;

public class out_pa {
    public static void command(Buddy buddy, String command, GuiChatWindow window, boolean withDelay) {
        ChatWindow.update_window(MessageType.RECEIVE_PAGE, window, command.substring(4), "", "", withDelay);
    }
}
