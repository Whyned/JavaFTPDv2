package ddrftpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;


public class SFVUtils {

	/*
	 * Returns a CRC32 Hashsum of File
	 * @return String: crc32 hashsum of File
	 */
    public static String CRC32File(File file) throws IOException {
    	Long checksum = null;
        InputStream fis = null;
        try
        {
          fis = new FileInputStream(file);
          CRC32 crc32 = new CRC32();
          byte[] buf = new byte[2048];
          int len;
          while ((len = fis.read(buf)) >= 0) {
            crc32.update(buf, 0, len);
          }
          checksum = Long.valueOf(crc32.getValue());
        } finally {
          if (fis != null) {
            fis.close();
          }
        }
        return longToHex(checksum);
    }

    /*
     * Converts Long to hex
     * @return String: hex
     */
	public static String longToHex(Long hex) {
	    String val = Long.toHexString(hex.longValue());
	    if (val.length() < 8) {
	      StringBuilder str = new StringBuilder();
	      for (int i = 0; i < 8 - val.length(); i++) {
	        str.append("0");
	      }
	      str.append(val);
	      val = str.toString();
	    }
	    return val;
	}

	/*
	 * Checks if File ends with .sfv
	 * @return boolean
	 */
	public static boolean isSFVFile(File file){
		if(file.isFile()){
			return file.getName().toLowerCase().endsWith(".sfv");
		}
		return false;
	}

	/*
	 * Creates File if it does not exist
	 */
	public static void createFileIfNotExists(File newfile) throws IOException{
		if(newfile.exists() == false){
			newfile.createNewFile();
		}
	}

	public static void removeFileIfExists(File f){
		if(f.exists() == true){
			f.delete();
		}
	}

	public static void removeFilesIfExist(File[] files){
		for(File f : files){
			removeFileIfExists(f);
		}
	}

	/* Looks for files ending with .sfv and returns them as File objects
	 * @return File[]
	 */
	public static File[] findSFVFiles(File folder){
		List<File> sfvfiles = new ArrayList<File>();
		File[] directoryListing = folder.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if(isSFVFile(child) == true){
					sfvfiles.add(child);
				}
			}

		}
		return sfvfiles.toArray(new File[sfvfiles.size()]);

	}

	/*
	 * Find hash to file
	 * @return String: null if nothing was found, else checksum
	 */

	public static String findFileChecksum(File parentFolder, String filename) throws IOException{
		for(File current_sfv : findSFVFiles(parentFolder)){
			SFVFileIterator sfvfile;
			try {
				sfvfile = new SFVFileIterator(current_sfv);
				while(sfvfile.hasNext()){
					String[] next = sfvfile.next();

					if(next[0].equals(filename) || (SFV.lowerfilenames.equals(true) && next[0].toLowerCase().equals(filename.toLowerCase()))){
						return next[1];
					}
				}
			} catch (FileNotFoundException e) {
				continue;
			}


		}
		return null;
	}

	public static String findFileChecksum(File file) throws IOException{
		return findFileChecksum(file.getParentFile(), file.getName());
	}


}
