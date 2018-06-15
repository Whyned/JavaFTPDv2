package ddrftpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class Quota{
	public HashMap<String, Long[]> quota = new HashMap<String, Long[]>();
	public String sizefile = "quota.size"; 
	
	public static void main(String[] args){
		System.out.println(humanReadableByteCount((long) 200000000, false));
	}
	
	public void setPath(String path){
		setPath(path, 0, 0);
		
	}
	public void setPath(String path, long limit){
		setPath(path, limit, 0);
	}
	public void setPath(String path, long limit, long size){
		Long[] q = {size, limit};
		quota.put(path, q);
		
	}
	public Long[] getQuota(String path){
		return quota.get(path);
	}
	
	public Long getQuota(String path, int index){
		Long[] q = getQuota(path);
		if(q != null){
			return q[index];
		}
		return null;
	
	}
	public void setQuota(String path, int index, long set){
		Long[] q = getQuota(path);
		if(q != null){
			q[index] = set;
		}
	}
	
	public void updateQuota(String path, int index, long update){
		Long[] q = getQuota(path);
		if(q != null){
			q[index] += update;
		}
	}
	public void setSize(String path, long size) throws FileNotFoundException, UnsupportedEncodingException{
		setQuota(path, 0, size);
		writeSizeFile();
	}
	
	public void setLimit(String path, long limit){
		setQuota(path, 1, limit);
	}
	
	public void addSize(String path, long add) throws FileNotFoundException, UnsupportedEncodingException{
		updateQuota(path, 0, +add);
		writeSizeFile();
	}
	
	public void subSize(String path, long sub) throws FileNotFoundException, UnsupportedEncodingException{
		updateQuota(path, 0, -sub);
		writeSizeFile();
	}
	public void addLimit(String path, long add){
		updateQuota(path, 1, +add);
	}
	
	public void subLimit(String path, long sub){
		updateQuota(path, 1, -sub);
	}
	public boolean isFull(String match){
		String matching = matchingPath(match);
		if(matching != null){
			Long[] quota = getQuota(matching);
			if(quota[0] >= quota[1]){
				return true;
			}
		}
		return false;
	}
	public String matchingPath(String match){
		match = match.replaceAll("\\./", "");
		String matching = null;
		for(String path : quota.keySet()){
			if(match.startsWith(path) && (matching == null || path.length() > matching.length())){
				matching = path;
			}
		}
		return matching;
	}

	public void writeSizeFile() throws FileNotFoundException, UnsupportedEncodingException{
		PrintWriter writer = new PrintWriter(sizefile, "UTF-8");
		for(Map.Entry<String, Long[]> pq : quota.entrySet()){
			writer.println(pq.getKey()+";"+pq.getValue()[0]);			
		}
		writer.close();
	}
	
	public void readSizeFile() throws FileNotFoundException, IOException{
		File f = new File(sizefile);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       String[] ps = line.split(";");
		       if(ps.length == 2 && quota.containsKey(ps[0])){
		    	   setSize(ps[0], Long.valueOf(ps[1]));
		    	   
		       }else{
		    	   Server.logger.warn("Path "+ps[0]+" is no longer a quota dir, therefore ignored. Should be fixed after next writeSizeFile()");
		       }
		    }
		}catch(FileNotFoundException e){
			f.createNewFile();
		}
	}
	public static String humanReadableByteCount(long bytes) {
		return humanReadableByteCount(bytes, false);
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	

}