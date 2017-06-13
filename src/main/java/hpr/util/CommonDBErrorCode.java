package hpr.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommonDBErrorCode {
/*
	public static enum Code { CONNECTION_FAIL, TABLE_NOT_EXIST, DUPLICATED_ROW, UNKOWN_ERROR};

	private static enum DB { ORACLE, MSSQL, MYSQL, CUBRID };
	private static Map<String, Map<Integer, Code>> instances_ = null;

	static {
		try {
			loadFromFile( "resource", "db-error-codes.properties");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private CommonDBErrorCode() {
	}

	public static Code getCode( String db, int errorCode ) {
		//지정된 에러코드 중 [CONNECTION_FAIL, TABLE_NOT_EXIST, DUPLICATED_ROW] 3개에 해당하지 않는 에러코드는
		//UNKOWN_ERROR 에러로 분류한다.
		if( null == instances_ )
			return Code.UNKOWN_ERROR;
		
		Code result = instances_.get( db ).get( errorCode );
		if(null == result)
			return Code.UNKOWN_ERROR;
		return result;
	}

	
	public static void loadFromFile( String pathName, String fileName ) throws IOException {
		

		File file = new File( pathName, fileName );

		FileInputStream istream = null;
		try {
			istream = new FileInputStream(file);
			
			Properties prop =  new Properties();
			prop.load(istream);

			instances_ = new HashMap<String, Map<Integer, Code>>();

			
			for( int i=0; i < DB.values().length; ++i ) {
				
				Map<Integer, Code> errorCodes = new HashMap<Integer, Code>();
				
				//DB_ERROR_CODE 에 정의된 Error 종류별로 루프롤 돈다.(마지막 UNKOWN_ERROR은 돌리지 않게 한다.)
				for( int j=1; j < Code.values().length; ++j ) {
					//properties 파일에 정의 된 DB Vender의 에러코드를 가져온다.
					String[] array = prop.getProperty( DB.values()[i].name().toLowerCase() + "_" + j ).split(",");

					//properties 파일에서 읽어온 Error 종류별로 errorCodes_ 에 넣는다.
					for( int k=0; k < array.length; ++k ) {
						//System.out.println("array[" + k + "] " + array[k]);
						if( array[k].isEmpty())
							continue;
						errorCodes.put( Integer.valueOf( array[k] ), Code.values()[j-1] );
					}
				}				
				instances_.put( DB.values()[i].name(), errorCodes );
			}
		}
		finally {
			if( null != istream ) {
				istream.close();
			}
		}
	}

	public static void main(String[] args) {
		System.out.println( "1->" + CommonDBErrorCode.getCode( "ORACLE", 2) );
		
	}
*/
}
