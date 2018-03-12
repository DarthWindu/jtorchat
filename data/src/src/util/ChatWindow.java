package util;

import java.io.FileOutputStream;
import java.io.IOException;

import core.Config;
import core.TCPort;
import gui.GuiChatWindow;

public class ChatWindow {

	//	update_window(type, w,new_entry,new_textarea,send,add_delay) {


	public static void update_window(MessageType type, GuiChatWindow window,String newEntry, String newTextArea, String send, boolean addDelay) {
		String delay="";

		if(addDelay)
			delay = "[Delayed] ";

		switch(type) {

		case PRIVATE:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Me", delay+"Private: ");
			window.addUrlText("Plain",newEntry + "\n");
			break;

		case REST:
			//Do nothing
			break;

			//RECEIVE functions

		case RECEIVE_ACTION:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Them", delay+"* " + window.b.toString()+" ");
			window.addUrlText("Plain",newEntry + "\n");
			break;

		case RECEIVE_NORMAL:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Them", delay+"Them: ");
			window.addUrlText("Plain",newEntry + "\n");
			break;

		case RECEIVE_PAGE:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Them", delay+"Them --> ");
			window.addUrlText("Plain",newEntry + "\n");
			break;

			//SEND functions

		case SEND_ACTION:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");

			if(TCPort.profile_name!=""){window.append("Me", delay+"* " + TCPort.profile_name+" ");}else
			{window.append("Me", delay+"* " + Config.us+" ");}
			window.addUrlText("Plain",newEntry + "\n");
			break;

		case SEND_NORMAL:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Me", delay+"Me: ");
			window.addUrlText("Plain",newEntry + "\n");
			break;

		case SEND_PAGE:
			window.append("Time Stamp", "(" + GuiChatWindow.getTime() + ") ");
			window.append("Me", delay+"Me --> ");
			window.addUrlText("Plain",newEntry.trim() + "\n");
			break;

		default:
			break;

		}

		window.get_textPane1().setCaretPosition(window.get_textPane1().getDocument().getLength());

		if(newTextArea != null)
			window.get_textArea4().setText(newTextArea);


		window.get_textArea4().requestFocusInWindow();

		/*
		 * COMMENT T1:
		 * 
		 * https://gist.github.com/DarthWindu/2f9eda0407cb85a107952a4262fef3cf
		 * 
		 * See gist above for a test case involving the following expression. Apparently, there is a situation where this expression CAN be appropriate (although I still
		 * recommend against it), so I'm going to leave this unless I can prove that it is indeed a bug as I initially
		 * thought.
		 * 
		 * - DW
		 */
		if(send!="") {	

			send = send.trim().replaceAll("\n", "\\\\n"/*this expresion evalutates to @"\\n" so i believe this is a bug, but I haven't tested yet*/).replaceAll("\r", "");

			try {
				if (!addDelay) {
					window.b.sendMessage(send);
				} else {
					FileOutputStream fos = new FileOutputStream(Config.MESSAGE_DIR + window.b.getAddress() + ".txt", true);
					fos.write(("[Delayed] "+send + "\n").getBytes());
					fos.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}



	}


}
