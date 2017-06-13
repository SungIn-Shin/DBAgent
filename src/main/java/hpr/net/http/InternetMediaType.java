package hpr.net.http;

public class InternetMediaType {
	
	public static String map (String fileExt) {
		
		fileExt = fileExt.toLowerCase();
		
		if (fileExt.equals("html") || fileExt.equals("htm")) {
			return "text/html";
		}
		else if (fileExt.equals("jpg")) {
			return "image/jpeg";
		}
		else if (fileExt.equals("gif")) {
			return "image/gif";
		}
		else if (fileExt.equals("css")) {
			return "text/css";
		}
		else if (fileExt.equals("js")) {
			return "application/javascript";
		}
		else if (fileExt.equals("json")) {
			return "application/json";
		}
		else 
			return "text/unknown";
	}
}
