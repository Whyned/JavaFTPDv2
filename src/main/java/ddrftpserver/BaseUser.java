package ddrftpserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * Generic user class. The user attributes are:
 * <ul>
 * <li>userid</li>
 * <li>userpassword</li>
 * <li>enableflag</li>
 * <li>homedirectory</li>
 * <li>writepermission</li>
 * <li>idletime</li>
 * <li>uploadrate</li>
 * <li>downloadrate</li>
 * </ul>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class BaseUser implements User {

    private String name = null;

    private String password = null;

    private int maxIdleTimeSec = 0; // no limit

    private String homeDir = null;

    private boolean isEnabled = true;
    
    private boolean isAdmin = false;
    
    private List<Authority> authorities = new ArrayList<Authority>();

    private List<String> permissions = new ArrayList<String>();
    
    /**
     * Default constructor.
     */
    public BaseUser() {
    }

    /**
     * Copy constructor.
     */
    public BaseUser(User user) {
        name = user.getName();
        password = user.getPassword();
        authorities = user.getAuthorities();
        maxIdleTimeSec = user.getMaxIdleTime();
        homeDir = user.getHomeDirectory();
        isEnabled = user.getEnabled();
    }

    /**
     * Get the user name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set user name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the user password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set user password.
     */
    public void setPassword(String pass) {
        password = pass;
    }

    public List<Authority> getAuthorities() {
        if (authorities != null) {
            return Collections.unmodifiableList(authorities);
        } else {
            return null;
        }
    }

    public void setAuthorities(List<Authority> authorities) {
        if (authorities != null) {
            this.authorities = Collections.unmodifiableList(authorities);
        } else {
            this.authorities = null;
        }
    }

    /**
     * Get the maximum idle time in second.
     */
    public int getMaxIdleTime() {
        return maxIdleTimeSec;
    }

    /**
     * Set the maximum idle time in second.
     */
    public void setMaxIdleTime(int idleSec) {
        maxIdleTimeSec = idleSec;
        if (maxIdleTimeSec < 0) {
            maxIdleTimeSec = 0;
        }
    }

    /**
     * Get the user enable status.
     */
    public boolean getEnabled() {
        return isEnabled;
    }

    /**
     * Set the user enable status.
     */
    public void setEnabled(boolean enb) {
        isEnabled = enb;
    }

    /**
     * Get the user home directory.
     */
    public String getHomeDirectory() {
        return homeDir;
    }

    /**
     * Set the user home directory.
     */
    public void setHomeDirectory(String home) {
        homeDir = home;
    }
    
    /**
     * Adds a permission
     * @param perm
     */
    public void addPermission(String perm){
    	permissions.add(perm);
    }
    /**
     * Checks if user has permission if user is admin return true
     * @param perm
     */
    public boolean hasPermission(String perm){
    	return hasPermission(perm, false);
    }
    
    public boolean hasPermission(String perm, boolean ignoreadmin){
    	if(ignoreadmin == false && isAdmin == true){
    		return true;
    	}
    	return permissions.contains(perm);
    }
    

    /**
     * Checks if user is Admin
     * @return
     */
    public boolean getAdmin(){
    	return isAdmin;
    }
    
    public void setAdmin(boolean adm){
    	isAdmin = adm;
    }
    
    /**
     * String representation.
     */
    public String toString() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationRequest authorize(AuthorizationRequest request) {
        // check for no authorities at all
        if(authorities == null) {
            return null;
        }
        
        boolean someoneCouldAuthorize = false;
        for (Authority authority : authorities) {
            if (authority.canAuthorize(request)) {
                someoneCouldAuthorize = true;

                request = authority.authorize(request);

                // authorization failed, return null
                if (request == null) {
                    return null;
                }
            }

        }

        if (someoneCouldAuthorize) {
            return request;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
        List<Authority> selected = new ArrayList<Authority>();

        for (Authority authority : authorities) {
            if (authority.getClass().equals(clazz)) {
                selected.add(authority);
            }
        }

        return selected;
    }

}
