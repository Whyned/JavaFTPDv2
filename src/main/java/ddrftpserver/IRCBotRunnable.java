package ddrftpserver;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import java.io.IOException;

public class IRCBotRunnable implements Runnable{
    private volatile boolean running = true;
    public PircBotX bot;

    public IRCBotRunnable(Configuration configuration) {
    	bot = new PircBotX(configuration);
    }
	
	public void terminate(){
		bot.stopBotReconnect();
		bot.sendIRC().quitServer();
		bot.close();
		running = false;
	}
	
	@Override
	public void run() {
		while(running){
			try {
				bot.startBot();
			} catch (IOException | IrcException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	

}
