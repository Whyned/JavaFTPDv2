package ddrftpserver;

import java.util.Date;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.User;


public class IRCBotMessageHandler {
	public String messageHandler(String message, User user, MessageSource source, PircBotX bot, IRCBotResponder responder) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		if(user.getChannels().isEmpty()){ // We ignore users which are not in a channel of the bot
			return null;
		}
				
		String commandLine = message;
		if(commandLine.startsWith("!")){
			commandLine = commandLine.substring(1);
		}else{
			// ToDo: Check for valid command if user is not authed...
			Pattern matchnamelist = Pattern.compile("^((\\s?[A-Za-z0-9-_\\[\\]{}^`|]+\\s?,)+)?(\\s?"+Pattern.quote(bot.getNick())+"\\s?,?)((\\s?[A-Za-z0-9-_\\[\\]{}^`|]+\\s?,?)+)?\\s+?!.+$");
			if(matchnamelist.matcher(commandLine).find()){
				commandLine = commandLine.substring(commandLine.indexOf("!")+1);
			}else{
				return null;
			}
		}
		parseCommand command = new parseCommand(commandLine);
		int i;
		switch(command.command){
		case "LOGIN":
			if(source == MessageSource.CHANNEL){
				return error("A bit dumb to authenticate in a public channel?!");
			}else if(IRCBot.authedUsers.contains(user.getNick())){
				return error("You are already logged in");
			}else if(command.hasArgument == false){
				return error("Need a password as argument...");
			}else{
				
				if(Server.ircbot.passwordEncryptorIRC.matches(command.joinArgument(), IRCBot.adminPassword)){
					IRCBot.authedUsers.add(user.getNick());
					return "Successfully logged in";
				}else{
					return error("Password wrong");
				}
				
			}
		case "LOGOUT":
			if(IRCBot.authedUsers.contains(user.getNick())){
				IRCBot.authedUsers.remove(user.getNick());
				return "Successfully logged out";
			}else{
				return error("You are not logged in");
			}
			
    	case "TIME":
			String time = new java.util.Date().toString();
			return("The time is now " + time);
		case "SEARCH":
        	FileSearch search = new FileSearch();
        	String searchString = command.joinArgument();
        	File Searchdir = new File("./home");
    		search.searchDirectory(Searchdir,searchString);
    		List<String> results = search.getResult();
    		if(responder instanceof IRCBotResponderDCC == false){
    			responder.response("Search for \""+searchString+"\" Found "+results.size()+ " results. Displaying first 10 results.");
    		}else{
    			responder.response("Search for \""+searchString+"\" Found "+results.size()+ " results.");
    		}
    		
    		i = 0;
    		for(String s : results){
    			if(i == 10 && responder instanceof IRCBotResponderDCC == false){
    				break;
    			}
    			responder.response(s.replace(Searchdir.getAbsolutePath(), ""));
    			i++;
    		}
    		break;
    	case "FREE":    		
    		if(Server.quota != null){
    			
    			i = 1;
    			for(Entry<String, Long[]> q : Server.quota.quota.entrySet()){
    				System.out.println("halloo");
    				Long[] sizes = q.getValue();
    				String path = q.getKey();
    				String pathname = path;
    				if(pathname.charAt(pathname.length()-1) == '/'){
    					pathname = pathname.substring(0, pathname.length()-1);
    				}
    				pathname = pathname.substring(pathname.lastIndexOf("/")+1, pathname.length());
    				responder.response("DISK `"+pathname+"`: "+Quota.humanReadableByteCount(sizes[0])+ " of "+Quota.humanReadableByteCount(sizes[1])+" used");
    				i++;
    			}
    		}
    		break;
    	case "VERSION":
    		return("Version: "+Server.version);

    	case "EXEC2":
    		if(IRCBot.authedUsers.contains(user.getNick())){
    			Runtime r = Runtime.getRuntime();
    			try{
    	       		r.exec(new String[]{ Server.shell, "-c", command.joinArgument()}, new String[]{});
    	       	}catch(Exception e){
    	       		return error("Execution failed: "+e);
    	       	}
    			return "Command executed successfully";
    		}else{
    			return error("Permission Denied");
    		}
    	case "SENDDCC":
            try {
				user.send().dccChat(true);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				Server.logger.error(e.getMessage());
				return(error(e.getMessage()));
			}
    	case "UPTIME":
    		Date now = new Date();
    		
    		long diff = now.getTime() - Server.serverFactory.serverContext.getFtpStatistics().getStartTime().getTime();
    		long diffSeconds = diff / 1000;         
    		long diffMinutes = diff / (60 * 1000);         
    		long diffHours = diff / (60 * 60 * 1000);         
    		long diffDays = diff / (60 * 60 * 24 * 1000);
    		long diffYears = diff / ( 60 * 60 * 24 * 365 * 1000);
    		return "Years: "+diffYears+" Days: "+diffDays+" Hours: "+diffHours+" Minutes: " +diffMinutes+" Seconds: "+diffSeconds;

    		
    	}
			
	
		return null;
	}
	
	public String error(String error){
		return "ERR: "+error;
	}
	
}
