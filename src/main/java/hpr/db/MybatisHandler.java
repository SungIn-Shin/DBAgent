package hpr.db;

import hpr.exception.DBException;
import hpr.exception.DBException.Code;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


public class MybatisHandler {
	
	private static Map<String, MybatisHandler> instance_ = new HashMap<String, MybatisHandler>();
	
	private SqlSessionFactory sqlSessionFactory_;
	private String databaseId_;
	
	private MybatisHandler	( String url
							, String userName
							, String passwd
							, String xmlFilePath
							, String mybatisConfig
							, String ... mappers
							) throws IOException {
			
		String rootPath = System.getProperty("user.dir");
		String fileSeparator = System.getProperty("file.separator");
		String fullPath = rootPath + fileSeparator;
		
		if( null != xmlFilePath && !xmlFilePath.isEmpty() ) {
			fullPath = fullPath + xmlFilePath + fileSeparator;
		}

		String mybatisConfigFile = fullPath + mybatisConfig;

		File file = new File( mybatisConfigFile );
		InputStream is = new FileInputStream(file);

		Properties dbProperties = new Properties();

//		dbProperties.setProperty( "driver"	, driver );
		dbProperties.setProperty( "url"		, url );
		dbProperties.setProperty( "userName", userName );
		dbProperties.setProperty( "passWord", passwd);
//		dbProperties.setProperty( "pingQuery", pingQuery);


		sqlSessionFactory_ = new SqlSessionFactoryBuilder().build( is, dbProperties );

		for( String mapper: mappers) {
			InputStream isMapper = Resources.getUrlAsStream(  "file:///" + fullPath + mapper);
			XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder( isMapper, sqlSessionFactory_.getConfiguration()
					, "file:///" + fullPath + mapper, sqlSessionFactory_.getConfiguration().getSqlFragments() );
			xmlMapperBuilder.parse();
		}

		databaseId_ = sqlSessionFactory_.getConfiguration().getDatabaseId().toUpperCase();
	}
	

	public List<Object> selectList( String namespace, String id, Object params ) throws DBException, Exception {
		
		List<Object> ret = null;
		for( int tryCnt = 0; tryCnt < 2; ++tryCnt ) {
			
			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory_.openSession();
			
				ret = sqlSession.selectList( namespace + "." + id, params );
				
				break;
			}
			catch( Exception ex ) {
				try {
					DBException.throwDBException( databaseId_, ex );
				}
				catch( DBException dbEx ) {
					if( 0 == tryCnt && dbEx.getCommonDBErrorCode() == Code.CONNECTION_FAIL ) {
						continue;
					}
					throw dbEx;
				}
				throw ex;
			}
			finally {
				sqlSession.close();
			}
		}
		
		return ret;
	}
		
	public Object selectOne( String namespace, String id, Object params ) throws DBException, Exception {
		
		Object ret = null;
		for( int tryCnt = 0; tryCnt < 2; ++tryCnt ) {
			
			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory_.openSession();
			
				ret = sqlSession.selectOne( namespace + "." + id, params );
				
				break;
			}
			catch( Exception ex ) {
				try {
					DBException.throwDBException( databaseId_, ex );
				}
				catch( DBException dbEx ) {
					if( 0 == tryCnt && dbEx.getCommonDBErrorCode() == Code.CONNECTION_FAIL ) {
						continue;
					}
					throw dbEx;
				}
				throw ex;
			}
			finally {
				sqlSession.close();
			}
		}
		
		return ret;
	}
		
	
	public int exec ( String namespace, String id, Object params ) throws DBException, Exception {
		
		int ret = -1;
		for( int tryCnt = 0; tryCnt < 2; ++tryCnt ) {
			
			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory_.openSession( true );
			
				ret = sqlSession.update(namespace + "." + id, params);
				
				break;
			}
			catch( Exception ex ) {
				try {
					DBException.throwDBException( databaseId_, ex );
				}
				catch( DBException dbEx ) {
					if( 0 == tryCnt && dbEx.getCommonDBErrorCode() == Code.CONNECTION_FAIL ) {
						continue;
					}
					throw dbEx;
				}
				throw ex;
			}
			finally {
				sqlSession.close();
			}
		}
		
		return ret;
	}
	
	public static MybatisHandler getInstance( String handlerId ) {
		
		MybatisHandler handler = instance_.get( handlerId );
		if( null == handler ) {
			throw new RuntimeException( "Unknown mybatis handler_id:" + handlerId );
		}
		return handler;
	}
	
	public static void makeMybatisHandler	( String handlerId
											, String url
											, String userName
											, String passwd
											, String configPath
											, String configFile
											, String ... mappers
											) throws IOException {
		
		
		MybatisHandler handler = new MybatisHandler(url, userName, passwd, configPath, configFile, mappers );
		instance_.put( handlerId, handler );
	}

	private static void test1(final MybatisHandler handler) {
		try {
			handler.exec( "ORACLE", "select_tab", null);
		} catch (DBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	private static void test2() {
		/*
		SqlSession sqlSession = DBHandler.getInstance().openSession(true);
		try {
			List<MessageVO> rows = sqlSession.selectList( 
					DBHandler.getInstance().databaseName_ + ".selectMsg", 4);
			
			for( MessageVO row : rows ) {
				System.out.println("rows:" + row);
			}
			
		}
		catch( Exception ex ) {
			
			
		}
		finally {
			sqlSession.close();
		}
		*/
	}

	// public static void main(String[] args) {
		
	// 	try {
	// 		//CommonDBErrorCode.loadFromFile( "resource", "db-error-codes.properties");

			
	// 		MybatisHandler.makeMybatisHandler	( "ims"
	// 											, "cfg", "mybatis-config.xml"
	// 											, "oracle.jdbc.driver.OracleDriver"
	// 											, "jdbc:oracle:thin:@192.168.0.213:1521:ims"
	// 											, "ism"
	// 											, "ism"
	// 											, "select * from dual"
	// 											, "oracle_test.xml"
	// 											);

	// 		test1( MybatisHandler.getInstance("ims"));
			

	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
					
	// }
}
