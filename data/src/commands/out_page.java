package commands;

import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;

import core.Buddy;
import core.Config;

public class out_page {
	public static void command(Buddy buddy, String s, GuiChatWindow window, boolean withDelay) {

		if(Config.pageactive == 1) {

			if (s.length() < 7) // When nothing is choosen use index
			{
				s="/page index";
			}

			s="/page "+s.substring(6).replaceAll("[^a-zA-Z]",""); // Replace all special letters

			String msg = util_page.read(s.substring(6));
			if(msg=="")
			{
				ChatWindow.update_window(MessageType.RECEIVE_PAGE, window,"He try to get '" + s.substring(6) + "' but it does not exist.","","/pa You try to get '" + s.substring(6) + "' but it does not exist.",withDelay);
			}
			else
			{
				ChatWindow.update_window(MessageType.RECEIVE_PAGE, window,"He get " + s.substring(6) + msg,"","/pa Get " + s.substring(6) + msg,withDelay);
			}		
		} else {
			ChatWindow.update_window(MessageType.PRIVATE, window,"He try to get something but the page system is not activated.","","/pa You try to get something but the page system is not activated..",withDelay);
		}

	}
}
