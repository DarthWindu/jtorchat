package commands;




import util.ChatWindow;
import util.MessageType;
import gui.GuiChatWindow;
import core.Buddy;

public class in_page_send {
	public static void command(Buddy buddy, String command, GuiChatWindow window){

		if (command.length() < 12) // When nothing is choosen use index
			command="/page_send index";
		

		command="/page_send "+command.substring(11).replaceAll("[^a-zA-Z]",""); // Replace all special letters

		String msg = util_page.read(command.substring(11));
		
		if(msg=="")//See comment T1 in ChatWindow
			ChatWindow.update_window(MessageType.PRIVATE, window,"The page '" + command.substring(11) + "' does not exist.","","",false);
		else
			ChatWindow.update_window(MessageType.SEND_PAGE, window,"Get '" + command.substring(11) + "'"+ msg,"","/pa Get " + command.substring(11) + msg,!buddy.isFullyConnected());


	}

}
