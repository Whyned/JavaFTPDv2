package ddrftpserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SFVInfo {


	/*
	 * Checks if file is complete and creates File infos ($filename-MISSING/-BAD)
	 */

	public static void generateFileInfo(File f, String checksum) throws IOException{
		SFVChecksumFile cf = new SFVChecksumFile(f, checksum);
		generateFileInfo(cf);

	}

	public static void generateFileInfo(SFVChecksumFile cf) throws IOException{
		String[] postfixes = {SFV.FILE_MISSING, SFV.FILE_BAD};
		int position = -1;
		if(cf.isMissing()){
			position = 0;
		}else if(cf.isBad()){
			position = 1;
		} // else -1
		createFilePostfixAndRemoveRest(postfixes, position, cf.getFile());
	}

	/*
	 * Creates File with $filename+postfix[position] and removes $filename+postfix[0<position>len(postfixes)]
	 */
	public static void createFilePostfixAndRemoveRest(String[] postfixes, int position, File f) throws IOException{
		File tmp;
		for(int i = 0; i < postfixes.length; i++){
			tmp = new File(f.getParentFile(), f.getName().toUpperCase()+postfixes[i]);
			if(i == position){
				SFVUtils.createFileIfNotExists(tmp);
			}else{
				SFVUtils.removeFileIfExists(tmp);
			}
		}
	}

	/*
	 * Generates [ 10 of 10 Files = 100% of 5MB] Directories
	 */
	public static void generateInfoDirectory(File folder, int missing, int bad, int total, long byte_filesize){
		int complete = total - missing - bad;
		new File(folder, "["+complete+" of "+total+" Files = "+ (int) ( total == 0 ? 0 : (complete * 100) / total) +"% of "+byte_filesize/1000000+"MB]").mkdirs();
	}

	/*
	 * Removes any Directory which matches Info directory style
	 */
	public static void cleanInfoDirectories(File folder){
		File[] directoryListing = folder.listFiles();
		if(directoryListing != null){
			for (File child: directoryListing){
				if(child.isDirectory() && child.getName().matches(SFV.REGEX_INFODIR)){
					child.delete();
				}
			}
		}
	}


	/*
	 * Creates (incomplete)-$dirname link in correct folder
	 */
	public static void createIncompleteLink(File folder) throws IOException{
		File link = getIncompleteLink(folder);
		if(link.exists() == false){
			Files.createSymbolicLink(link.toPath(), folder.getAbsoluteFile().toPath());
		}

	}

	/*
	 * Cleans up (incomplete)-$dirname links
	 */
	public static void cleanIncompleteLink(File folder){
		File link = getIncompleteLink(folder);
		if(link.exists() == true){
			link.delete();
		}
	}

	/*
	 * Returns correct parent folder for incomplete links
	 */
	public static File getIncompleteParentFolder(File folder){
		String[] lvl2parentmatch = {"CD", "Proof"};
		File parentFolder = folder.getParentFile();
		for(String m : lvl2parentmatch){
			if(folder.getName().startsWith(m)){
				parentFolder = parentFolder.getParentFile();
				break;
			}
		}
		return parentFolder;
	}

	/*
	 * Get Incomplete Link
	 */
	public static File getIncompleteLink(File folder){
		return new File(getIncompleteParentFolder(folder), SFV.LINK_INCOMPLETE+folder.getName());

	}

	public static void cleanAllSFVInfos(File folder){
		File[] directoryListing = folder.listFiles();
		if(directoryListing != null){
			String name;
			for (File child: directoryListing){
				name = child.getName();

				if((child.isDirectory() && name.matches(SFV.REGEX_INFODIR)) || name.endsWith(SFV.FILE_BAD) || name.endsWith(SFV.FILE_MISSING)){
					child.delete();
				}
			}
		}
		cleanIncompleteLink(folder);
	}
}
