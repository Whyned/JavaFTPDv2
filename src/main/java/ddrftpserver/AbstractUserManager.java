package ddrftpserver;

import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Abstract common base type for {@link UserManager} implementations
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractUserManager implements UserManager {

    public static final String ATTR_LOGIN = "userid";

    public static final String ATTR_PASSWORD = "userpassword";

    public static final String ATTR_HOME = "homedirectory";

    public static final String ATTR_WRITE_PERM = "writepermission";

    public static final String ATTR_ENABLE = "enableflag";

    public static final String ATTR_MAX_IDLE_TIME = "idletime";

    public static final String ATTR_MAX_UPLOAD_RATE = "uploadrate";

    public static final String ATTR_MAX_DOWNLOAD_RATE = "downloadrate";

    public static final String ATTR_MAX_LOGIN_NUMBER = "maxloginnumber";

    public static final String ATTR_MAX_LOGIN_PER_IP = "maxloginperip";
    
    public static final String ATTR_IS_ADMIN = "admin";
    
    public static final String ATTR_PERMISSIONS = "permissions";

    private List<String> adminNames  = new ArrayList<String>();;
    
    private PasswordEncryptor passwordEncryptor = new Md5PasswordEncryptor();


    /**
     * Internal constructor, do not use directly
     */
    public AbstractUserManager(PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }
    
    public void addAdmin(String name){
    	adminNames.add(name);
    }
    /**
     * Get the admin names.
     */
    public String getAdminName() {
        return adminNames.toArray().toString();
    }

    /**
     * Returns all Admin Names as array
     */
    public String[] getAdminNames() {
    	return (String[]) adminNames.toArray();
    }
    
    
    /**
     * @return true if user with this login is administrator
     */
    public boolean isAdmin(String login) throws FtpException {
        return adminNames.contains(login);
    }
    
    
    /**
     * Retrieve the password encryptor used for this user manager
     * @return The password encryptor. Default to {@link Md5PasswordEncryptor}
     *  if no other has been provided
     */
    public PasswordEncryptor getPasswordEncryptor() {
        return passwordEncryptor;
    }
}