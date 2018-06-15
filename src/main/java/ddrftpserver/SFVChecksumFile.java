package ddrftpserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SFVChecksumFile {

	private File file;
	private String hash = "";
	private String hashcomplete = "";

	public SFVChecksumFile(File f) throws IOException{
		hashFile(f);
	}

	public SFVChecksumFile(File f, String hashcomplete) throws IOException{
		hashFile(f);
		setCompleteHash(hashcomplete);

	}

	public void hashFile(File f) throws IOException{
		file = f;

		try {
			hash = SFVUtils.CRC32File(file).toLowerCase();
		} catch (FileNotFoundException e) {
			// Do nothing, isMissing will tell
		}
	}

	public File getFile(){
		return file;
	}

	public void setCompleteHash(String complete){
		hashcomplete = complete.toLowerCase();
	}

	public String getCompleteHash(){
		return hashcomplete;
	}

	public String getHash(){
		return hash;
	}

	public boolean isMissing(){
		return !file.exists();
	}

	public boolean isBad(){
		return !isComplete();
	}

	public boolean isComplete(){
		return hash.equals(hashcomplete);
	}

	/*
	 * 0 Missing
	 * 1 Bad
	 * 2 Complete
	 */
	public int _case(){
		if(isComplete()){
			return 2;
		}else if(isMissing()){
			return 0;
		}else{
			return 1;
		}
	}



}
