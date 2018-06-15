package ddrftpserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.pircbotx.User;
import org.pircbotx.dcc.ReceiveChat;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.IncomingChatRequestEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;


public class IRCBotListenerAdapter extends ListenerAdapter{
	
		
		
		private IRCBotMessageHandler handler = new IRCBotMessageHandler();
		
		@Override
		public void onPart(PartEvent event){
			IRCBot.authedUsers.removeUserIfExists(event.getUser());
		}
		
		@Override
		public void onKick(KickEvent event){
			IRCBot.authedUsers.removeUserIfExists(event.getUser());
		}
	
		@Override
		public void onNickChange(NickChangeEvent event){
			String nold = event.getOldNick();
			String nnew = event.getNewNick();
			if(IRCBot.authedUsers.contains(nold)){
				IRCBot.authedUsers.remove(nold);
				IRCBot.authedUsers.add(nnew);
			}
		}
		@Override
		public void onMessage(MessageEvent event) throws NoSuchAlgorithmException, UnsupportedEncodingException{
			User user = event.getUser();
			
			String response = handler.messageHandler(event.getMessage(), user, MessageSource.CHANNEL, event.getBot(), new IRCBotResponderNormal(event));
			if(response != null){
				event.respond(response);
			}
		}

		@Override
		public void onPrivateMessage(PrivateMessageEvent event) throws NoSuchAlgorithmException, UnsupportedEncodingException{
			User user = event.getUser();
			String response = handler.messageHandler(event.getMessage(), user, MessageSource.PRIVATE, event.getBot(), new IRCBotResponderNormal(event));
			if(response != null){
				event.respond(response);
			}
		}
		
	    @Override
	    public void onIncomingChatRequest(IncomingChatRequestEvent event) throws IOException, NoSuchAlgorithmException  {
	    	
	        //Accept the request. This actually opens the connection. Remember that 
	        //the user may have a time limit on how long you have to accept the request
	        ReceiveChat chat = event.accept();
	        User user = event.getUser();
	        //Basic read loop. This is similar to reading a stream
	        String line;
	        IRCBotResponder responder = new IRCBotResponderDCC(chat);
	        //Keep reading lines until the line is null, signaling the end of the connection
	        while ((line = chat.readLine()) != null) {
	            //Just send back what they said
	        	String response = handler.messageHandler(line, user, MessageSource.DCC, event.getBot(), responder);
	        	if(response != null){
	        		responder.response(response);
	        	}
	        }
	    }
	

}
