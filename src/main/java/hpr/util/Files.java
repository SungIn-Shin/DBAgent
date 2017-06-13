package hpr.util;

import java.io.File;

public class Files {
	public static String getFileNameExtention (String fullpath) {
		
		if( null == fullpath )
			return "";

		int idx = fullpath.lastIndexOf(".");
		if (-1 == idx)
			return "";

		return fullpath.substring(idx+1);
	}

	public static String getFileNameWithoutExtention (String fullpath) {
		
		if( null == fullpath )
			return "";

		int idx = fullpath.lastIndexOf(".");
		if (-1 == idx)
			return "";

		return fullpath.substring(0, idx);
	}
	
	
	public static String getFileName (String fullpath) {
		
		if( null == fullpath )
			return "";

		int idx = fullpath.lastIndexOf(System.getProperty("file.separator"));
		if (-1 == idx)
			return fullpath;

		return fullpath.substring(idx+1);
	}
	
	public static String getPath (String fullpath) {
		
		if( null == fullpath )
			return "";

		int idx = fullpath.lastIndexOf(System.getProperty("file.separator"));
		if (-1 == idx)
			return "";

		return fullpath.substring(0, idx);
	}
	
	public static void fileDelete( String deleteFileName) {
        File file = new File(deleteFileName);
        
        if(file.exists()){
            file.delete();
        }
    }
	
// 	public static void main(String[] args) {
		
// //		String fullpath = "ccc.txt";
// //		String fullpath = "c:\\aaa\\bbb\\ccc.txt";
// 		String fullpath = "bbb\\ccc.txt";
// 		System.out.println(getPath(fullpath));
// 		System.out.println(getFileName(fullpath));
// 		System.out.println(getFileNameExtention(fullpath));
// 		System.out.println(getFileNameWithoutExtention(fullpath));
// 	}	
}
