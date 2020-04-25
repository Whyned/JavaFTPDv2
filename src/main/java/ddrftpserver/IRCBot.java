package ddrftpserver;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.PircBotX.State;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.delay.StaticDelay;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

public class IRCBot{
    private Thread thread = null;
    private IRCBotRunnable runnable = null;
	
  public static boolean enabled = false;
	public static String adminPassword = "someadminpassword";
	public static IRCAuthenticatedUsers authedUsers = new IRCAuthenticatedUsers();
	public static PircBotX bot = null;
	public static String prefix = null;
	public String[] channels = {"#channelname chanpass"};
	public String announcechannel = "#channelnametoannounce";
	public String preprefix = "[PREFIX]";
	public String hostname = "someircnetword.irc";
	public Integer port = 71337;
	public PasswordEncryptor passwordEncryptorIRC = new ClearTextPasswordEncryptor();

	
	
	public static void main(String[] args) throws InterruptedException{
    	IRCBot bot = new IRCBot();
    	bot.start();
    	System.out.println("test");
    	Thread.sleep(60*100);
    	System.out.println("shutdown");
    	System.out.println(bot.status());
    	bot.stop();
    	

	}
	public void start(){
        //Configure what we want our bot to do
        Configuration configuration = new Configuration.Builder()
                .setName(preprefix+ (prefix != null ? "-"+prefix : "") + "-"+generateRand()) //Set the nick of the bot. CHANGE IN YOUR CODE
                .setRealName("DDR")
                .setVersion("DDRFTPD v"+Server.version)
                .setLogin(preprefix+ (prefix != null ? "-"+prefix : ""))
                .addServer(hostname, port)
                .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
                .addAutoJoinChannel("#DDRiderZ.Str0s sheeeesh")
                .addListener(new IRCBotListenerAdapter())
                .setAutoReconnect(true)
                .setAutoReconnectDelay(new StaticDelay((long) 10000))
                .setAutoReconnectAttempts(999999999)
                .setMessageDelay(new StaticDelay((long) 2))//Add our listener that will be called on Events
                
                .buildConfiguration();

        
        runnable = new IRCBotRunnable(configuration);
        thread = new Thread(runnable);
        thread.start();

        
	}
	
	public void stop() throws InterruptedException{
		if(thread != null){
			runnable.terminate();
			thread.join();
		}
		
	}
	
	public State status(){
		return runnable.bot.getState();
	}



	public void announce(String s){
		if(runnable != null){
			Server.logger.info("[ANNOUNCE] "+announcechannel+" :"+s);
			runnable.bot.sendIRC().message(announcechannel, s);
		}
	}



	public String generateRand(){
		String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String r = "";
		Random rnd = new SecureRandom();
		for( int i = 0; i < 5; i++ ){
			      r+=( AB.charAt( rnd.nextInt(AB.length()) ) );
		}
		return r;
	}
}
