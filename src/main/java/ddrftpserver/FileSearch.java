package ddrftpserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
 
public class FileSearch {
 
  public String fileNameToSearch;
  public List<String> result = new ArrayList<String>();
  public int searchMethod = 1; 
  public int maxdepth = -1;
  public int limit = -1;
  public int hits = 0;
  public String getFileNameToSearch() {
	return fileNameToSearch;
  }
 
  public void setFileNameToSearch(String fileNameToSearch) {
	this.fileNameToSearch = fileNameToSearch;
  }
  /*
   * int i
   *     0 = regex
   *     1 = contains
   */
  public void setMethod(int i){
	  searchMethod = i;
  }
  
  public int getMethod(){
	  return searchMethod;
  }
 
  public List<String> getResult() {
	return result;
  }
  public static void main(String[] args){
	  FileSearch fileSearch = new FileSearch();
	  fileSearch.limit = 1;
	  fileSearch.searchDirectory(new File("/tmp"), "qt");
      int count = fileSearch.getResult().size();
	  if(count ==0){
		    System.out.println("\nNo result found!");
	}else{
		    System.out.println("\nFound " + count + " result!\n");
		    for (String matched : fileSearch.getResult()){
			System.out.println("Found : " + matched);
		    }
		}
  }

  public void searchDirectory(File directory, String fileNameToSearch) {
 
	setFileNameToSearch(fileNameToSearch);
    search(directory, 0);

 
  }

  private void search(File file, int depth) {
	  if(limit != -1 && hits >= limit){
		  return;
	  }
	  String name = file.getName();
	  String path = file.getAbsoluteFile().toString();
      boolean match = nameMatches(name);
      boolean depthok = false;
      if(maxdepth == -1 || depth < maxdepth){
    	  depthok = true;
      }
	  if(file.isDirectory() && file.canRead() && depthok){
			for (File temp : file.listFiles()) {
				search(temp, depth+1);
			}
	  }
	  if(match){
		  result.add(path);
		  hits++;
	  }
  }
  
  private boolean nameMatches(String name){
	  boolean match = false;
	  if(searchMethod == 0){
		  match = name.matches(fileNameToSearch);
	  }else if(searchMethod == 1){
		  match = name.contains(fileNameToSearch);
	  }
	  return match;
  }
 
}
