package ddrftpserver;

import java.io.IOException;

import org.pircbotx.dcc.ReceiveChat;

public class IRCBotResponderDCC implements IRCBotResponder {

	ReceiveChat chat;
	public IRCBotResponderDCC(ReceiveChat c){
		chat = c;
	}
	@Override
	public void response(String s) {
		// TODO Auto-generated method stub
		try {
			chat.sendLine(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Server.logger.error("Could not Send DCC Response:\n"+e.getMessage());
		}
	}
	
}