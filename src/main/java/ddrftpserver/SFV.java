package ddrftpserver;

import java.io.File;
import java.io.IOException;



public class SFV {
	public static String FILE_BAD = "-BAD";
	public static String FILE_MISSING = "-MISSING";
	public static String LINK_INCOMPLETE = "(incomplete)-";
	public static String ANNOUNCE_COMPLETE = "[COMPLETE]";
	public static String REGEX_INFODIR = "\\[(.*)of(.*)Files =(.*)of(.*)MB\\]";
	public static Boolean lowerfilenames = true; // lowerCase filenames, some old mp3 releases need this
	private static File lastcomplete = null;

	public synchronized static void registerFile(File file) throws IOException{
		File parentFile = file.getParentFile();
		if(SFVUtils.isSFVFile(file) == true){
			// It's an SFV File so go threw all files in sfv

			SFVFileIterator sfvfile = new SFVFileIterator(file);
			try{
				while(sfvfile.hasNext()){
					String[] sfv = sfvfile.next();
					File fileInSFV;
					if(SFV.lowerfilenames == true){
						File lowerFileName = new File(file.getParentFile(), sfv[0].toLowerCase());
						if(lowerFileName.exists()){
							fileInSFV = lowerFileName;
						}else{
							fileInSFV = new File(file.getParentFile(), sfv[0]);
						}
					}else{
						fileInSFV = new File(file.getParentFile(), sfv[0]);
					}
					SFVInfo.generateFileInfo(fileInSFV, sfv[1]);
				}

			}finally{
				sfvfile.close();
			}
		}else{
			// It's an normal file, check if we have any sfv files and if any of those contains a hash for us
			String checksum = SFVUtils.findFileChecksum(file);
			if(checksum == null){
				// File is not in any .sfv file, so nothing to do
				return;
			}else{
				SFVInfo.generateFileInfo(file, checksum);
			}

		}
		//ToDo: Generate info dir
		generateInfoDirs(parentFile, true);

	}

	public synchronized static void unregisterFile(File file) throws IOException{
		File parentFile = file.getParentFile();
		String[] postfixes = {FILE_BAD, FILE_MISSING};
		if(SFVUtils.isSFVFile(file) == true){
			// Delete all files ending with any of our -MISSING/-BAD tags
			for(File f : parentFile.getAbsoluteFile().listFiles()){
				for(String p : postfixes){
					if(f.getName().endsWith(p)){
						f.delete();
						break;
					}
				}

			}

			// And don't miss the incomplete link
			SFVInfo.cleanIncompleteLink(parentFile.getAbsoluteFile());

		}else{
			if(SFVUtils.findFileChecksum(file) != null){
				// File is in SFV

				SFVInfo.createFilePostfixAndRemoveRest(postfixes, 1, file);
				generateInfoDirs(parentFile, true);
			}
		}

	}

	public synchronized static void generateInfoDirs(File folder, boolean announce) throws IOException{
		SFVFileIterator sfvfile;
		String[] sfv;
		Integer total = 0;
		Integer complete = 0;
		Integer bad = 0;
		long file_size = 0;
		File file;
		File lowerfile;
		Boolean fileexists;
		for(File sfvf : SFVUtils.findSFVFiles(folder)){
			sfvfile = new SFVFileIterator(sfvf);
			try{

				while(sfvfile.hasNext()){
					sfv = sfvfile.next();
					file = new File(folder, sfv[0]);
					fileexists = file.exists();
					total++;
					if(SFV.lowerfilenames && fileexists == false){
						lowerfile = new File(folder, sfv[0].toLowerCase());
						if(lowerfile.exists()){
							file = lowerfile;
							fileexists = true;
						}
					}

					if(fileexists == true){
						if(new File(folder, sfv[0].toUpperCase()+FILE_BAD).exists() == true){
							bad++;
						}else if(new File(folder, sfv[0].toUpperCase()+FILE_MISSING).exists() == false){
							complete++;
							file_size += file.length();
						}

					}

				}
			}finally{
				sfvfile.close();
			}

		}
		int missing = total - complete - bad;
		// First clean up info dir
		SFVInfo.cleanInfoDirectories(folder);

		// Next generate a new one
		SFVInfo.generateInfoDirectory(folder, missing, bad, total, file_size);

		// If complete kill (incomplete) folder and announce

		if(missing == 0 && bad == 0){
			SFVInfo.cleanIncompleteLink(folder);
			if(announce == true && Server.ircbot != null && (lastcomplete == null || lastcomplete.equals(folder) == false)){
				lastcomplete = folder;
				Server.ircbot.announce(ANNOUNCE_COMPLETE+" "+folder.getName());

			}
		}else{
			SFVInfo.createIncompleteLink(folder);
		}


	}


}
