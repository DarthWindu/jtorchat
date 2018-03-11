package commands;

import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;
import core.Buddy;
import core.language;

public class in_help {
    public static void command(Buddy buddy, String command, GuiChatWindow window) {

        String help;
        help = "\n" + language.langtext[57] + "\n";
        help += "/page_disp site\n";
        help += "/page_send site\n";
        help += "/page site\n";
        help += "/log\n";

        ChatWindow.update_window(MessageType.PRIVATE, window, help, "", "", false);

    }
}
