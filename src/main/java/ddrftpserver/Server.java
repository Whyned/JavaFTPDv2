package ddrftpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

import org.ini4j.Wini;
import org.apache.log4j.*;

public class Server {
    public static Logger logger = Logger.getLogger(Server.class);

    public static UserManager um;
    public static FtpServerFactory serverFactory;
    public static ListenerFactory factory;
    public static SslConfigurationFactory ssl;
    public static PropertiesUserManager userManager;
    public static ConnectionConfigFactory connectionConfigFactory;
    public static DataConnectionConfigurationFactory dataConnectionFactory;
    public static FtpServer server;
    public static Wini inifile;
    public static Quota quota;
    public static String shell = "/bin/sh";
    public static SFV sfv;
    public static IRCBot ircbot;
    public static String version = "0.3.4";

	public static void main(String[] args) throws FtpException, URISyntaxException, FileNotFoundException, IOException {
		setup();
		start();



	}
	public static void setup() throws URISyntaxException, FileNotFoundException, IOException{
		init();
		config();
		set();
	}
	public static void init(){
		// Init all things, we dont do this in header so we can restart easier
		serverFactory = new FtpServerFactory();
		factory = new ListenerFactory();
	  ssl = new SslConfigurationFactory();
		connectionConfigFactory = new ConnectionConfigFactory();
		dataConnectionFactory = new DataConnectionConfigurationFactory();
		ircbot = new IRCBot();
	}
	public static void config() throws URISyntaxException, FileNotFoundException, IOException{
		String configfile = "config.ini";
		logger.info("Loading config from \""+configfile+"\"");
		try {
			inifile = new Wini(new File(configfile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn("Could not load "+configfile+", Using default configs");

		}
		// Log4j

		if(ini_has("Log4j", "file")){
			Properties logProperties = new Properties();
			logProperties.load(new FileInputStream(inifile.get("Log4j", "file")));
		    PropertyConfigurator.configure(logProperties);
		}
		// Server
		factory.setPort(ini_has("Server", "port") ? inifile.get("Server", "port", int.class) : 2221);
		if(ini_has("Server", "address")){
			factory.setServerAddress(inifile.get("Server", "address"));
		}
		if(ini_has("Server", "passiveports")){
			dataConnectionFactory.setPassivePorts(inifile.get("Server", "passiveports"));
		}
		if(ini_has("Server", "shell")){
			shell = inifile.get("Server", "shell");
		}else{
			//TODO: add OS identification
			shell = "/bin/sh";
		}

		// SSL
		if(ini_has("SSL", "enable") == true && inifile.get("SSL", "enable", boolean.class) == true){
			// TODO: Add more ssl configuration stuff
			// To generate an own keystorefile run:
			//   keytool -genkeypair -alias client -keystore clientkeystore -keyalg rsa
			ssl.setSslProtocol(ini_has("SSL", "protocol") ? inifile.get("SSL", "protocol") : "TLS");
			ssl.setKeystoreFile(ini_has("SSL", "keystorefile") ? new File(inifile.get("SSL", "keystorefile")) : null);
			ssl.setKeystorePassword(ini_has("SSL", "keystorepassword") ? inifile.get("SSL", "keystorepassword") : "password");
			factory.setImplicitSsl(ini_has("SSL", "implicitssl") ? inifile.get("SSL", "implicitssl", boolean.class) : false);

		}else{
			ssl = null;
		}

		// Users
		File userDataFile = null;
		if(ini_has("Users", "userfile")){
			userDataFile = new File(inifile.get("Users", "userfile"));
			String encryptstr = ini_has("Users", "passwordencryption") ? inifile.get("Users", "passwordencryption") : "";
			PasswordEncryptor passwordEncryptor = config_parsePasswordEncryptionString(encryptstr);
			userManager = new PropertiesUserManager(passwordEncryptor, userDataFile);

		}else{
			userManager = new PropertiesUserManager(new ClearTextPasswordEncryptor(), new File("users.properties"));
		
		}




		// Quota
		if(ini_has("Quota", "quotas")){
			quota = new Quota();
			String[] quotas = inifile.get("Quota", "quotas").split(";");
			String[] pl;

			for(String q : quotas){
				String path = null;
				Long limit = (long) 0;
				pl = q.split(":");
				if(pl.length == 2){
					path = pl[0];
					limit = Long.valueOf(pl[1]);
					if(!path.startsWith("/")){
						path = System.getProperty("user.dir") + path.replaceFirst("./", "/");
					}
					quota.setPath(path, limit);
				}else{
					logger.warn("Quota->quotas is in wrong format ("+q+")");
				}
			}
			if(ini_has("Quota", "sizefile")){
				quota.sizefile = inifile.get("Quota", "sizefile");
			}
			quota.readSizeFile();

		}else{
			quota = null;
		}

		// SFV
		if(ini_has("SFV", "enable") && inifile.get("SFV", "enable", boolean.class) == false){
			sfv = null;
		}else{
			sfv = new SFV();
        if(ini_has("SFV", "lowerfilenames") && inifile.get("SFV", "lowerfilenames", boolean.class) == false){
           SFV.lowerfilenames = false;
        }
    }


		// IRC
		if(ini_has("IRC", "prefix")){
			IRCBot.prefix = inifile.get("IRC", "prefix");
		}
		if(ini_has("IRC", "adminpw")){
			IRCBot.adminPassword = inifile.get("IRC", "adminpw");
		}
		if(ini_has("IRC", "passwordencryption")){
			String encryptstrirc = inifile.get("IRC", "passwordencryption");
			Server.ircbot.passwordEncryptorIRC = config_parsePasswordEncryptionString(encryptstrirc);

		}


	}
	public static PasswordEncryptor config_parsePasswordEncryptionString(String encryptstr){
		PasswordEncryptor passwordEncryptor;
		switch(encryptstr){
		case "md5":
			passwordEncryptor = new Md5PasswordEncryptor();
			break;
		case "clear":
			passwordEncryptor = new ClearTextPasswordEncryptor();
			break;
		case "md5-salted":
			passwordEncryptor = new SaltedPasswordEncryptor();
			break;
		default:
			passwordEncryptor = new ClearTextPasswordEncryptor();
			break;
		}

		return passwordEncryptor;
	}
	public static void set(){
		// Set stuff
		serverFactory.getFtplets().put("ftplet", new FTPLet());
		factory.setDataConnectionConfiguration(dataConnectionFactory.createDataConnectionConfiguration());
		//factory.setSslConfiguration();
		if(ssl != null){
				factory.setSslConfiguration(ssl.createSslConfiguration());
		}
		serverFactory.addListener("default", factory.createListener());
		serverFactory.setUserManager(userManager);
		serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
		server = serverFactory.createServer();

	}

	public static boolean ini_has(Object sectionName, Object optionName){
		if(inifile == null || inifile.get(sectionName, optionName) == null){
			return false;
		}
		return true;

	}


	public static void stop(){
		logger.info("Stopping Server...");
		server.stop();

	}
	public static void start() throws FtpException{
		// start the server
		logger.info("Starting Server...");
		server.start();
	}

	public static void restartApplication() throws URISyntaxException, IOException
	{
	  final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	  final File currentJar = new File(Server.class.getProtectionDomain().getCodeSource().getLocation().toURI());

	  /* is it a jar file? */
	  if(!currentJar.getName().endsWith(".jar"))
	    return;

	  /* Build command: java -jar application.jar */
	  final ArrayList<String> command = new ArrayList<String>();
	  command.add(javaBin);
	  command.add("-jar");
	  command.add(currentJar.getPath());

	  logger.info("Restarting Application...");
	  final ProcessBuilder builder = new ProcessBuilder(command);
	  builder.start();
	  System.exit(0);
	}

}
