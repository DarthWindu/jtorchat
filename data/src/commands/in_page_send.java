package commands;




import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;
import core.Buddy;

public class in_page_send {
	public static void command(Buddy buddy, String s, GuiChatWindow window){

		if (s.length() < 12) // When nothing is choosen use index
			s="/page_send index";
		

		s="/page_send "+s.substring(11).replaceAll("[^a-zA-Z]",""); // Replace all special letters

		String msg = util_page.read(s.substring(11));
		
		if(msg=="")//See comment T1 in ChatWindow
			ChatWindow.update_window(MessageType.PRIVATE, window,"The page '" + s.substring(11) + "' does not exist.","","",false);
		else
			ChatWindow.update_window(MessageType.SEND_PAGE, window,"Get '" + s.substring(11) + "'"+ msg,"","/pa Get " + s.substring(11) + msg,!buddy.isFullyConnected());


	}

}
