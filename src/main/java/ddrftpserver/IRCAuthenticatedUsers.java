package ddrftpserver;

import java.util.ArrayList;

import org.pircbotx.User;

public class IRCAuthenticatedUsers extends ArrayList<String> {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean add(String user){
		return super.add(user.toLowerCase());
	}

	public boolean contains(String user){
		return super.contains(user.toLowerCase());
	}

	public boolean remove(String user){
		return super.remove(user.toLowerCase());
	}

	public boolean removeUserIfExists(User user){
		String nick = user.getNick();
		if(this.contains(nick)){
			this.remove(user);
			return true;
		}
		return false;
	}

}
