package ddrftpserver;

import java.util.ArrayList;

import org.apache.ftpserver.ftplet.FtpRequest;

public class parseCommand{
	public String command = null;
    public String requestLine;
	public String[] requestLineSplitted;
	public ArrayList<String> arguments = new ArrayList<String>();
	public boolean hasArgument;
	public boolean isSite = false;
	
	private int argStart = 1; // Normally 0 is command, and args are 1+

	parseCommand(FtpRequest request){
		this(request.getRequestLine());

	}
	
	parseCommand(String string){
		/*
		 * System.out.println(requestLineSplitted[0]);
		int argparsestart = 1;
		
		if(requestLineSplitted[0].toUpperCase().equals("SITE")){
			isSite = true;
		}
		if(isSite){
			
			command = requestLineSplitted[1];
			argparsestart = 2;
			if(requestLineSplitted.length > 0){
				hasArgument = true;
			}
		}else{
			command = requestLineSplitted[0];
			
		}
		command = command.toUpperCase();
		for (int i = argparsestart; i < requestLineSplitted.length; i++) {
			String arg = requestLineSplitted[i];
			if(arg.startsWith("-")){
				arg = arg.toLowerCase();
			}
			arguments.add(arg);
		}
		 */
		requestLine = string;
		requestLineSplitted = requestLine.split(" ");
		
		if(requestLineSplitted[0].toUpperCase().equals("SITE")){
			argStart = 2;
		}
		
		// Set command
		/*
		 * 0 SITE 1
		 * 1 HELP 2
		 * 2 ARG1 3
		 * 3 ARG2 4
		 */
		if(requestLineSplitted.length >= argStart){
			command = requestLineSplitted[argStart-1].toUpperCase();
		}
		
		// Check if we have args
		if(requestLineSplitted.length >= argStart+1){
			hasArgument = true;
		}
		
		// Now build arg arraylist
		for (int i = argStart; i < requestLineSplitted.length; i++) {
			String arg = requestLineSplitted[i];
			if(arg.startsWith("-")){
				arg = arg.toLowerCase();
			}
			arguments.add(arg);
		}
		
		
		
		
	}
	
	public boolean hasArg(String arg){
		return arguments.contains(arg.toLowerCase());
	}

	public boolean commandIs(String cmd){
		if(command.equals(cmd)){
			return true;
		}
		return false;
	}
	
	public String joinArgument(){
		return joinArgument(0, arguments.size()-1, " ");
	}
	public String joinArgument(int start, int end, String c){
		if(hasArgument == false){
			return null;
		}
		String r = "";
		int i = start;
		while(i <= end){
			r += c;
			r += arguments.get(i);
			i += 1;
		}
		return r.substring(1);
	}

}
