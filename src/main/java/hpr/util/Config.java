package hpr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class Config {

	//public static final String PATH_NAME = "cfg";

	private static Config config_ = null;
	
	private Properties properties_;
	private String fileName_;
	
	private Config() throws IOException {
		properties_ = new Properties();
	}
	
	public Config loadFromFile( final String fileName ) throws IOException {
		fileName_ = fileName;
		loadFromFile();
		return this;	
	}

	public synchronized String get( String key, String def ) {
		if (fileName_.isEmpty()) { return def; }
		return properties_.getProperty( key, def );
	}

	public synchronized String get( String key ) throws IOException {
		if (fileName_.isEmpty()) { return ""; }

		String ret = properties_.getProperty( key );
		if( null == ret ) {
			throw new IOException("Failed to find key: " + key);
		}
		return ret;
	}

	public synchronized void set( String key, String value ) throws IOException {
		if (fileName_.isEmpty()) { return; }

		properties_.setProperty( key, value );
	
		saveToFile();
	}
	
	public synchronized String toString() {
		return properties_.toString();
	}
	
	private void loadFromFile () throws IOException {

		File file = null;
		InputStream input = null;
		
		try {
			
			file = new File( /*PATH_NAME, */fileName_ );
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			input = new FileInputStream( file);

			properties_.clear();
			properties_.load( input );
		}
		finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {}
			}
		}		
	}
	
	private void saveToFile () throws IOException {
		OutputStream output = null;

		try {
			File file = new File( /*PATH_NAME, */fileName_ );
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			
			output = new FileOutputStream(  file );

			properties_.store( output, null );
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {}
			}
		}		
	}
	

	public static Config cfg() throws IOException {
	
		if (null == config_) {
			config_ = new Config();
		}
		return config_;
	}
	
	
	// public static void main(String[] args) {

	// 	try {
	// 		Config.getInstance("aaa.txt").put("aaa", "111");
	// 		Config.getInstance("aaa.txt").put("bbb", "221");
			
	// 		System.out.println("aaa:" + Config.getInstance("aaa.txt").get("aaa"));
	// 		System.out.println("bbb:" + Config.getInstance("aaa.txt").get("bbb"));
			
	// 		System.out.println(Config.getInstance("aaa.txt").toString());
	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
	// }
	
}


/*
private Preferences prefs_;

private Config() {
	prefs_ = Preferences.userNodeForPackage(this.getClass());
}

public String get( String key, String def ) {
	return prefs_.get( key, def );
}

public void put( String key, String value ) {
	prefs_.put( key, value );
}

public int getInt (String key, int def) {
	return prefs_.getInt(key, def);
}
	
public void putInt ( String key, int value ) {
	prefs_.putInt( key, value );
}
*/
