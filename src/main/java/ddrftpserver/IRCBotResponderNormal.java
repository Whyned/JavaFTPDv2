package ddrftpserver;

import org.pircbotx.hooks.Event;

public class IRCBotResponderNormal implements IRCBotResponder {

	Event event;
	public IRCBotResponderNormal(Event e){
		event = e;
	}
	@Override
	public void response(String s) {
		// TODO Auto-generated method stub
		event.respond(s);
	}
	
}
