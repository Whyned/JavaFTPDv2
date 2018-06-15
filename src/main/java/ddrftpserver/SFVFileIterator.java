package ddrftpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SFVFileIterator {
	public File sfv;
	public BufferedReader br;
	private String[] next = null;

	SFVFileIterator(File sfvfile) throws FileNotFoundException{
		sfv = sfvfile;
		br = new BufferedReader(new FileReader(sfvfile));

	}


	public boolean hasNext() throws IOException{
		String line = null;
		String[] tmp = null;
		while((line = br.readLine()) != null){
			if(line.startsWith("//") || line.startsWith(";") || line.length() == 0){
				continue;
			}else if(line.contains(" ")){
				tmp = line.split(" ");
				if(tmp[1].length() != 8){
					continue;
				}
				next = tmp;
				return true;
			}
		}
		return false;

	}

	public String[] next(){
		return next;
	}

	public void close() throws IOException{
		br.close();
	}
}
