package hpr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CodeMapper {
	
	private static Map<String, CodeMapper> instance_ = new HashMap<String, CodeMapper>();
	
	private Properties properties_;
	
	
	private CodeMapper( String pathName, String fileName ) throws IOException {
	
		File file = new File( pathName, fileName );
		FileInputStream istream = null;
		
		try {
			istream = new FileInputStream(file);
			properties_ =  new Properties();
			properties_.load(istream);
		}
		finally {
			if( null != istream ) {
				try {
					istream.close();
				}
				catch( IOException ex ) {}
			}
		}
	}

	public String getCode( String key ) {
		return getCode( key, "" );
	}
	
	public String getCode( String key, String defaultValue ) {
		String result = (String) properties_.get(key);
		if( null == result ) {
			return defaultValue;
		}
		return result;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for( Entry<Object, Object> entry: properties_.entrySet() ) {
			sb.append( entry.getKey() );
			sb.append( "=>" );
			sb.append( entry.getValue() );
			sb.append( " " );
		}
		return sb.toString();
	}
	
	public static CodeMapper getInstance( String id ) {
		CodeMapper codeMapper = instance_.get( id );
		if( null == codeMapper ) {
			throw new RuntimeException( "Unknown CodeMapper id:" + id );
		}
		return codeMapper;
	}
	
	public static void make( String id, String pathName, String fileName ) throws IOException {
		CodeMapper codeMapper = new CodeMapper( pathName, fileName );
		instance_.put( id, codeMapper );
	}
	
	
	// public static void main(String[] args) {

	// 	try {
	// 		CodeMapper.make("NET_CODE", "resource", "net_code.map");
			
	// 		System.out.println("LGU -> " + CodeMapper.getInstance("NET_CODE").getCode("LGU"));
	// 		System.out.println("ALL:" + CodeMapper.getInstance("NET_CODE"));
			
	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
	// }
}
