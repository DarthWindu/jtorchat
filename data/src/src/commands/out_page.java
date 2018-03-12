package commands;

import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;

import core.Buddy;
import core.Config;

public class out_page {
    public static void command(Buddy buddy, String command, GuiChatWindow window, boolean withDelay) {
        /*I'm guessing that this string (second parameter called $s previously) is a command. Not sure->rename as necessary
        *
        * Commit Date: Almost 2AM EST on 3/11/18 --> or should I say almost 3 *sigh*
        * */
        if (Config.pageactive == 1) {

            if (command.length() < 7) {// When nothing is choosen use index
                command = "/page index";
            }

            command = "/page " + command.substring(6).replaceAll("[^a-zA-Z]", ""); // Replace all special letters

            String msg = util_page.read(command.substring(6));
            if (msg == "") {
                ChatWindow.update_window(MessageType.RECEIVE_PAGE, window, "He try to get '" + command.substring(6) + "' but it does not exist.", "", "/pa You try to get '" + command.substring(6) + "' but it does not exist.", withDelay);
            } else {
                ChatWindow.update_window(MessageType.RECEIVE_PAGE, window, "He get " + command.substring(6) + msg, "", "/pa Get " + command.substring(6) + msg, withDelay);
            }
        } else {
            ChatWindow.update_window(MessageType.PRIVATE, window, "He try to get something but the page system is not activated.", "", "/pa You try to get something but the page system is not activated..", withDelay);
        }

    }
}
