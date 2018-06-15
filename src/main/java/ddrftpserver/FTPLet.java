package ddrftpserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.pircbotx.PircBotX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FTPLet extends DefaultFtplet{
	public FtpletContext ftpletContext;
	public DefaultFtpReply PERMISSION_DENIED = new DefaultFtpReply(550, "Permission Denied");
	static Logger log = LoggerFactory.getLogger("FTPlet");

	public static final String ATTRIBUTE_PREFIX = "ddrftpserver.";
	
	private static final String ATTRIBUTE_RENAME_FROM_SIZE = ATTRIBUTE_PREFIX + "rename-from-size";
	private static final String ATTRIBUTE_RENAME_FROM_QUOTA_PATH = ATTRIBUTE_PREFIX + "rename-from-quota-path";
	private static final String ATTRIBUTE_RENAME_TO_QUOTA_PATH = ATTRIBUTE_PREFIX + "rename-to-quota-path";
	private static final String ATTRIBUTE_RENAME_FROM_PATH = ATTRIBUTE_PREFIX + "rename-from-path";
	private static final String ATTRIBUTE_FILE_SIZE = ATTRIBUTE_PREFIX + "file-size";
	private static final String ATTRIBUTE_FILE_QUOTA_PATH = ATTRIBUTE_PREFIX + "file-quota-path";

	@Override
	public void init(FtpletContext paramftpletContext) throws FtpException {
		ftpletContext = paramftpletContext;

		// Start irc
		if(Server.ircbot != null){
			Server.ircbot.start();
		}
    }

	@Override
	public void destroy(){
		if(Server.ircbot != null && Server.ircbot.status() == PircBotX.State.CONNECTED){
			Server.ircbot.announce("Arrgh!, See Ya Chum!");
			try {
				Server.ircbot.stop();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
		String FileName = request.getArgument();
		long FileSize = ftpFileSize(session, request);
		String FileRealPath = ftpRealFilePath(session, request);
		if(Server.quota != null){
			String quotaPath = Server.quota.matchingPath(FileRealPath);
			if(quotaPath != null){
				if(Server.quota.isFull(quotaPath) == true){
					session.write(new DefaultFtpReply(552, FileName+": Disk Full/Quota exceeded"));
					return FtpletResult.SKIP;
				}
				if(FileSize > 0){
					// Its no new file, probably append or overwrite, therefore we sub the file size from quota
					Server.quota.subSize(quotaPath, FileSize);
				}

			}

		}
		if(Server.sfv != null){
			String upperFileName = FileName.toUpperCase();
			if(upperFileName.endsWith(SFV.FILE_BAD) || upperFileName.endsWith(SFV.FILE_MISSING)){
				session.write(new DefaultFtpReply(553, FileName+": Filename not allowed, conflicting SFV Check"));
				return FtpletResult.SKIP;
			}
		}
		return FtpletResult.DEFAULT;
	}
	@SuppressWarnings("static-access")
	@Override
	public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		long FileSize = ftpFileSize(session, request);
		String FileRealPath = ftpRealFilePath(session, request);
		if(Server.quota != null){
			String quotaPath = Server.quota.matchingPath(FileRealPath);
			if(quotaPath != null){
				Server.quota.addSize(quotaPath, FileSize);
			}
		}
		if(Server.sfv != null){
			Server.sfv.registerFile(ftpRealFile(session, request));
		}
		return FtpletResult.DEFAULT;
	}

	@Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
		

		if(Server.quota != null){
			long FileSize = ftpFileSize(session, request);
			session.setAttribute(ATTRIBUTE_FILE_SIZE, FileSize);
		}



		return FtpletResult.DEFAULT;

    }

    @SuppressWarnings("static-access")
	public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request) throws FtpException, IOException{
    	if(Server.quota != null || Server.sfv != null){
    		File FileReal = ftpRealFile(session, request);
        	if(Server.quota != null){
    			String quotaPath = Server.quota.matchingPath(FileReal.getAbsolutePath());
    			if(quotaPath != null){
    				Server.quota.subSize(quotaPath, (long) session.getAttribute(ATTRIBUTE_FILE_SIZE)); 
    			}
        	}
    		if(Server.sfv != null){
    			Server.sfv.unregisterFile(FileReal);
    		}
    	}

		return FtpletResult.DEFAULT;
    }


		@Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException{
			if(Server.sfv != null){
				File FileReal = ftpRealFile(session, request);
				String FileName = FileReal.getName();
				if(FileName.matches(SFV.REGEX_INFODIR) || FileName.startsWith(SFV.LINK_INCOMPLETE)){
					session.write(new DefaultFtpReply(553, FileName+": Directory name not allowed, conflicting SFV Infodir"));
					return FtpletResult.SKIP;
				}

			}
			return FtpletResult.DEFAULT;

		}

    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException{
		File FileReal = ftpRealFile(session, request);
		if(Server.ircbot != null && FileReal.exists()){
			Server.ircbot.announce("[MKD] "+FileReal.getName());
		}
		return FtpletResult.DEFAULT;

    }

		@Override
		public FtpletResult onRmdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException{
			File FileReal = ftpRealFile(session, request);
			if(FileReal.isDirectory()){
				if(FileReal.list().length>0){
					// Directory is not empty, very likely because of sfv files so clean em up, otherwise we get problems later
					SFVInfo.cleanAllSFVInfos(FileReal);
				}
			}
			return FtpletResult.DEFAULT;
		}

    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException{
		File FileReal = ftpRealFile(session, request);
		if(Server.ircbot != null && !FileReal.exists()){
			Server.ircbot.announce("[RMD] "+FileReal.getName());
		}
		return FtpletResult.DEFAULT;
	}
    
    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request) throws FtpException, IOException{
		
    	if(Server.quota != null){
    		
    		String quotaPathFrom = Server.quota.matchingPath(ftpRealFilePath(session, session.getRenameFrom()));
    		String quotaPathTo = Server.quota.matchingPath(ftpRealFilePath(session, request));
    		if((quotaPathFrom != null || quotaPathTo != null) || !quotaPathFrom.equals(quotaPathTo)){ // we don't move inside the same quota disk
    			
    			long FileSize;
    			if(session.getRenameFrom().isFile() == true){
    				FileSize = session.getRenameFrom().getSize();
    			}else{
    				File realFile = ftpRealFile(session, session.getRenameFrom());
    				FileSize = size(realFile.toPath());
    			}
    			
    			if(quotaPathTo != null){
    				Long[] diskQuota = Server.quota.getQuota(quotaPathTo);
    				if(diskQuota[0] + FileSize > diskQuota[1]){
        				session.write(new DefaultFtpReply(553,"Can't Move File, Quota exceeded"));
        				return FtpletResult.SKIP;
        			}
    			}
       			session.setAttribute(ATTRIBUTE_RENAME_FROM_QUOTA_PATH, quotaPathFrom);
    			session.setAttribute(ATTRIBUTE_RENAME_TO_QUOTA_PATH, quotaPathTo);
    			session.setAttribute(ATTRIBUTE_RENAME_FROM_SIZE, FileSize);   				
    			
    		}

    	}
    	if(Server.ircbot != null){
    		session.setAttribute(ATTRIBUTE_RENAME_FROM_PATH, session.getRenameFrom().getAbsolutePath());
    	}
		return FtpletResult.DEFAULT;
    	
    }
    
    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException, FileNotFoundException, UnsupportedEncodingException{
    	if(Server.quota != null){
    		if(session.getAttribute(ATTRIBUTE_RENAME_FROM_SIZE) instanceof Long){
    			long FileSize = (long) session.getAttribute(ATTRIBUTE_RENAME_FROM_SIZE);
    			session.removeAttribute(ATTRIBUTE_RENAME_FROM_SIZE);
        		if(session.getAttribute(ATTRIBUTE_RENAME_FROM_QUOTA_PATH) instanceof String){
        			String quotaPathFrom = (String) session.getAttribute(ATTRIBUTE_RENAME_FROM_QUOTA_PATH);
        			session.removeAttribute(ATTRIBUTE_RENAME_FROM_QUOTA_PATH);
        			if(quotaPathFrom != null){
            			Server.quota.subSize(quotaPathFrom, FileSize);
        			}


        		}
        		if(session.getAttribute(ATTRIBUTE_RENAME_TO_QUOTA_PATH) instanceof String){
        			String quotaPathTo = (String) session.getAttribute(ATTRIBUTE_RENAME_TO_QUOTA_PATH);
        			session.removeAttribute(ATTRIBUTE_RENAME_TO_QUOTA_PATH);
        			if(quotaPathTo != null){
        				Server.quota.addSize(quotaPathTo, FileSize);
        			}
        			
        		}

    		}		

    	}
    	if(Server.ircbot != null){
    		Server.ircbot.announce("[CP] "+session.getAttribute(ATTRIBUTE_RENAME_FROM_PATH)+" - "+request.getArgument());
    	}
		return FtpletResult.DEFAULT;
    }

	@Override
	public FtpletResult onSite(FtpSession session, FtpRequest request) throws FtpException, IOException {
		User user = session.getUser();
		FtpletResult FtpletReturn = null;
		parseCommand cmd = new parseCommand(request);
		//TODO: search, makesalted, umrechner
		switch(cmd.command){
		case "RELOAD":
			FtpletReturn = SiteCommandRELOAD(session, user);
			break;
		case "RESTART":
			FtpletReturn = SiteCommandRESTART(session, user);
			break;
		case "FREE":
			FtpletReturn = SiteCommandFREE(session, request, user);
			break;
		case "EXEC":
			FtpletReturn = SiteCommandEXEC(session, request, user, cmd);
			break;
		case "RESCAN":
			FtpletReturn = SiteCommandRESCAN(session, request, user);
			break;
		case "SEARCH":
			FtpletReturn = SiteCommandSEARCH(session, request, user, cmd);
			break;
		case "ENCRYPT":
			FtpletReturn = SiteCommandENCRYPT(session, user, cmd);
			break;
		}
        return FtpletReturn == null ? FtpletResult.DEFAULT : FtpletReturn;


	}
	public FtpletResult SiteCommandRELOAD(FtpSession session, User user) throws FtpException{
		if(user.hasPermission("reload") == false){
            session.write(PERMISSION_DENIED);
		}else{
			String response = "Reloading Server...";
			session.write(new DefaultFtpReply(200, response));
			Server.ircbot.announce("[INFO] " +response);
			Server.stop();
			Server.init();
			boolean success = true;
			try{
				Server.config();
			}catch(Exception e){
				success = false;
				session.write(new DefaultFtpReply(555, "Failed to reload config"));
			}
			if(success){
				Server.set();
				Server.start();
				session.write(new DefaultFtpReply(200, "Successfully reloaded"));
			}
		}
		return FtpletResult.SKIP;

	}

	public FtpletResult SiteCommandFREE(FtpSession session, FtpRequest request, User user) throws FtpException, IOException{

		if(user.hasPermission("free") == false){
			session.write(PERMISSION_DENIED);
		}else{
			String free = "Path | Used | Limit";
			if(Server.quota != null){
				Set<Entry<String, Long[]>> quotas = Server.quota.quota.entrySet();
				if(quotas.size() > 0){
					for(Map.Entry<String, Long[]> pq : quotas ){
						free += "\n"+ pq.getKey().replace(System.getProperty("user.dir"), ".")+" "+Quota.humanReadableByteCount(pq.getValue()[0])+" "+Quota.humanReadableByteCount(pq.getValue()[1]);
					}
				}
			}
			File currentDir = ftpWorkingDir(session); // We want current folder
			free+= "\n .    "+Quota.humanReadableByteCount((currentDir.getTotalSpace()-currentDir.getFreeSpace()))+" "+Quota.humanReadableByteCount(currentDir.getTotalSpace());
			session.write(new DefaultFtpReply(220, free));
		}
		return FtpletResult.SKIP;
	}

	public FtpletResult SiteCommandEXEC(FtpSession session, FtpRequest request, User user, parseCommand cmd) throws FtpException, IOException{
		if(user.hasPermission("exec") == false){
			session.write(PERMISSION_DENIED);
		}else{
			Runtime r = Runtime.getRuntime();
	       	String workingDir = ftpWorkingDir(session).getCanonicalPath();
	       	try{
	       		r.exec(new String[]{ Server.shell, "-c", cmd.joinArgument()}, new String[]{}, new File(workingDir));
	       	}catch(Exception e){
	       		session.write(new DefaultFtpReply(556, "Execution failed: "+e));
	       		return FtpletResult.SKIP;
	       	}
	        session.write(new DefaultFtpReply(200, "Command executed"));
		}
        return FtpletResult.SKIP;
	}

	@SuppressWarnings("static-access")
	public FtpletResult SiteCommandRESCAN(FtpSession session, FtpRequest request, User user) throws FtpException, IOException{
		if(user.hasPermission("rescan") == false){
			session.write(PERMISSION_DENIED);
		}else{
			if(Server.sfv == null){
				session.write(new DefaultFtpReply(200, "SFV Disabled"));
			}else{
				File currentFolder = ftpWorkingDir(session);
				log.debug("!RESCAN: current Folder: "+currentFolder);
				// First we delete all -BAD & -MISSING Files, just to make sure we get everything clean
				String fname;
				for(File f : currentFolder.listFiles()){
					fname = f.getName();
					if(fname.endsWith(SFV.FILE_MISSING) || fname.endsWith(SFV.FILE_BAD)){
						f.delete();
					}
				}

				String r = "Filename |  SFV CRC  |  FILE CRC  | Status";
				int missing = 0;
				int total = 0;
				int bad = 0;
				int complete = 0;

				SFVFileIterator sfvfile;
				String[] line;
				SFVChecksumFile checksum;
				String status;
				File file;
				File lowerfile;
				for(File f : SFVUtils.findSFVFiles(currentFolder)){
					sfvfile = new SFVFileIterator(f);
					while(sfvfile.hasNext()){
						line = sfvfile.next();
						file = new File(currentFolder, line[0]);
						if(!file.exists() && SFV.lowerfilenames){
							lowerfile = new File(currentFolder, line[0].toLowerCase());
							if(lowerfile.exists()){
								file = lowerfile;
							}
						}

						checksum = new SFVChecksumFile(file, line[1]);
						SFVInfo.generateFileInfo(checksum);
						status = "MISSING";
						total++;
						switch(checksum._case()){
						case 0:
							missing++;
							break;
						case 1:
							bad++;
							status = "BAD";
							break;
						case 2:
							complete++;
							status = "COMPLETE";
							break;
						}
						r+="\n"+line[0]+"  "+line[1]+"  "+(checksum.isMissing() ? "NULL    " : checksum.getHash())+"  "+status;


					}
				}
				r+="\n---------------------\nComplete: "+complete+"\nBad: "+bad+"\nMissing: "+missing+"\nTotal: "+total+"\n"+(total == complete ? "Everything OK" : "Nothing OK! Get your shit complete!");
				Server.sfv.generateInfoDirs(currentFolder, false);
				session.write(new DefaultFtpReply(200, r));
			}

		}
		return FtpletResult.SKIP;

	}

	public FtpletResult SiteCommandSEARCH(FtpSession session, FtpRequest request, User user, parseCommand cmd) throws FtpException, IOException{
		if(user.hasPermission("search") == false){
			session.write(PERMISSION_DENIED);
		}else{

			File workingDir = ftpWorkingDir(session);
			String searchString = cmd.joinArgument();
			if(searchString.isEmpty() == true){
				session.write(new DefaultFtpReply(557, "No Search String specified"));
			}else{
				String response = "Searching in "+workingDir+" for \""+searchString+"\":";
				FileSearch search = new FileSearch();
				search.searchDirectory(workingDir, searchString);
				List<String> results = search.getResult();
				if(results.isEmpty()){
					response += "\nNothing found";
				}else{
					for(String s : results){
						response += "\n"+s;
					}
				}
				session.write(new DefaultFtpReply(200, response));
			}
		}

		return FtpletResult.SKIP;

	}

	public FtpletResult SiteCommandRESTART(FtpSession session, User user) throws FtpException{
		if(user.hasPermission("restart") == false){
			session.write(PERMISSION_DENIED);
			return FtpletResult.SKIP;
		}else{
			String response = "Restarting Server...";
			session.write(new DefaultFtpReply(200, response));
			Server.ircbot.announce("[INFO] " +response);
			try {
				Server.restartApplication();
			} catch (URISyntaxException | IOException e) {
				// TODO Auto-generated catch block
				session.write(new DefaultFtpReply(507, "Couldn't restart Application: "+e.getMessage()));
				log.error("Couldn't restart Application: "+e.getMessage());
			}
		}
		return FtpletResult.DISCONNECT;
	}

	public FtpletResult SiteCommandENCRYPT(FtpSession session, User user, parseCommand cmd) throws FtpException{
		if(user.hasPermission("restart") == false){
			session.write(PERMISSION_DENIED);
			return FtpletResult.SKIP;
		}else{
			if(cmd.arguments.size() < 1){
				session.write(new DefaultFtpReply(300, "To less arguments"));
			}else if(cmd.arguments.size() > 3){
				session.write(new DefaultFtpReply(300, "To many arguments"));
			}else{
				int method = 0; // 0 => md5 1 => salted
				String password = "";
				String salt = null;
				String encrypted_str = "";
				if(cmd.arguments.size() == 1 || cmd.arguments.get(0).toLowerCase().equals("md5")){
					//md5
				}else if(cmd.arguments.get(0).toLowerCase().equals("salted")){
					method = 1;
					if(cmd.arguments.size() == 2){
						password = cmd.arguments.get(1);
					}else if(cmd.arguments.size() == 3){
						salt = cmd.arguments.get(1);
						password = cmd.arguments.get(2);
					}
				}else{
					session.write(new DefaultFtpReply(300, "Don't know what ya want?!"));
					return FtpletResult.SKIP;
				}
				switch(method){
				case 0:
					encrypted_str = new Md5PasswordEncryptor().encrypt(password);
					break;
				case 1:
					encrypted_str = (salt == null ? new SaltedPasswordEncryptor().encrypt(password) : new SaltedPasswordEncryptor().encrypt(password, salt));
				}
				session.write(new DefaultFtpReply(200, "Encryption: "+(method == 0 ? "MD5" : "Salted")+"\nPassword (plain): "+password+"\nSalt: "+salt+"\nEncrypted Password: "+encrypted_str));

			}

		}
		return FtpletResult.SKIP;
	}

	/**
	 * Returns a File object with the real and absolute destination
	 * @param session
	 * @param ftpFile
	 * @return
	 * @throws FtpException
	 * @throws IOException
	 */
	public File ftpRealFile(FtpSession session, FtpFile ftpFile) throws FtpException, IOException{
		String filePath = session.getUser().getHomeDirectory() + ftpFile.getAbsolutePath();
		return ftpRealFile(session, filePath);
	}
	
	public File ftpRealFile(FtpSession session, String filePath) throws IOException{		
		return new File(filePath).getCanonicalFile();
	}

	public File ftpRealFile(FtpSession session, FtpRequest request) throws FtpException, IOException{
		return ftpRealFile(session, ftpFile(session, request));
	}


	/**
	 * Returns a FtpFile which does not represent the real destination on the Filesystem
	 * @param session
	 * @param request
	 * @return
	 * @throws FtpException
	 */
	public FtpFile ftpFile(FtpSession session, FtpRequest request) throws FtpException{
		return session.getFileSystemView().getFile(request.getArgument());
	}
	
	/**
	 * Returns the file size of a ftprequest file
	 * @param session
	 * @param request
	 * @return
	 * @throws FtpException
	 */
	public long ftpFileSize(FtpSession session, FtpRequest request) throws FtpException{
		return session.getFileSystemView().getFile(request.getArgument()).getSize();
	}
	

	public String ftpRealFilePath(FtpSession session, FtpRequest request) throws FtpException, IOException{
		return ftpRealFile(session, request).getAbsolutePath();
	}
	
	public String ftpRealFilePath(FtpSession session, String filePath) throws IOException{
		return ftpRealFile(session, filePath).getAbsolutePath();
	}
	
	public String ftpRealFilePath(FtpSession session, FtpFile ftpFile) throws IOException, FtpException{
		return ftpRealFile(session, ftpFile).getAbsolutePath();
	}

	public File ftpWorkingDir(FtpSession session) throws FtpException, IOException{
		return ftpRealFile(session, session.getFileSystemView().getWorkingDirectory());
	}
	public String[] parseCommand(String line){
		return line.split(" ");
	}
	
	  public static long size (Path path) {

          final AtomicLong size = new AtomicLong(0);

          try
          {
              Files.walkFileTree (path, new SimpleFileVisitor<Path>() 
              {
                    @Override public FileVisitResult 
                  visitFile(Path file, BasicFileAttributes attrs) {

                          size.addAndGet (attrs.size());
                          return FileVisitResult.CONTINUE;
                      }

                    @Override public FileVisitResult 
                  visitFileFailed(Path file, IOException exc) {

                          Server.logger.debug("skipped: " + file + " (" + exc + ")");
                          // Skip folders that can't be traversed
                          return FileVisitResult.CONTINUE;
                      }

                    @Override public FileVisitResult
                  postVisitDirectory (Path dir, IOException exc) {

                          if (exc != null)
                              Server.logger.error("had trouble traversing: " + dir + " (" + exc + ")");
                          // Ignore errors traversing a folder
                          return FileVisitResult.CONTINUE;
                      }
              });
          }
          catch (IOException e)
          {
              throw new AssertionError ("walkFileTree will not throw IOException if the FileVisitor does not");
          }

          return size.get();
      }
}
