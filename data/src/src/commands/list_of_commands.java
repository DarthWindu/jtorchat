package commands;

import gui.GuiChatWindow;
import core.Buddy;

public class list_of_commands {

    public static boolean in_command(Buddy buddy, String origCommand, GuiChatWindow guiChatWindow) {
        String command = origCommand.split(" ")[0];
        if (command.equals("/log")) {
            in_log.command(buddy, origCommand, guiChatWindow);
            return false;
        } else if (command.equals("/help")) {
            in_help.command(buddy, origCommand, guiChatWindow);
            return false;
        } else if (command.equals("/me")) {
            in_me.command(buddy, origCommand, guiChatWindow);
            return false;
        } else if (command.equals("/page_disp")) {
            in_page_disp.command(buddy, origCommand, guiChatWindow);
            return false;
        } else if (command.equals("/page_send")) {
            in_page_send.command(buddy, origCommand, guiChatWindow);
            return false;
        }
        return true;
    }

    public static boolean out_command(Buddy buddy, String origCommand, GuiChatWindow guiChatWindow, boolean with_delay) {
        String command = origCommand.split(" ")[0];

        if (command.equals("/me") ||
                command.equals("/pa") ||
                command.equals("/page")) {

            out_me.command(buddy, origCommand, guiChatWindow, with_delay);
            return false;
        }


//When the command is not exist then it can be not jtorchat because you can not send commands yourself
        return true;
    }

}
