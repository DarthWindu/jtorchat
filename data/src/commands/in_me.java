package commands;

import gui.GuiChatWindow;
import util.ChatWindow;
import util.MessageType;
import core.Buddy;

public class in_me {
    public static void command(Buddy buddy, String command, GuiChatWindow window) {

        if (command.length() < 5)
            ChatWindow.update_window(MessageType.PRIVATE, window, "Parameter /me msg", "", "", false);
        else
            ChatWindow.update_window(MessageType.SEND_ACTION, window, command.substring(4), "", "/me " + command.substring(4), !buddy.isFullyConnected());
    }
}
