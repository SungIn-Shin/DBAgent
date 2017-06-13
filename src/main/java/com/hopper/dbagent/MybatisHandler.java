package com.hopper.dbagent;

import com.hopper.dbagent.DBException;
import com.hopper.dbagent.DBException.Code;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.executor.BatchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MybatisHandler {

	private static Map<String, MybatisHandler> instance_ = new HashMap<String, MybatisHandler>();

	private static final Logger log = LoggerFactory.getLogger(MybatisHandler.class);

	private SqlSessionFactory sqlSessionFactory_;
	private String databaseId_;

	private MybatisHandler( String environment
		, String dbUrl
		, String dbUserName
		, String dbPasswd
		, String mybatisXml
		, String ... mappers
		) throws Exception {


		Properties dbProperties = new Properties();

		// dbProperties.setProperty( "driver"	, driver );
		dbProperties.setProperty( "url"		, dbUrl );
		dbProperties.setProperty( "userName", dbUserName );
		dbProperties.setProperty( "passWord", dbPasswd);
		InputStream is = new FileInputStream(new File(mybatisXml));

		sqlSessionFactory_ = new SqlSessionFactoryBuilder().build( is, environment, dbProperties ); 

		for( String mapper: mappers) {
			InputStream isMapper = new FileInputStream(new File(mapper));

			XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder( isMapper, sqlSessionFactory_.getConfiguration()
				, mapper, sqlSessionFactory_.getConfiguration().getSqlFragments() );
			xmlMapperBuilder.parse();
		}
		
		databaseId_ = environment.toUpperCase();
		// databaseId_ = sqlSessionFactory_.getConfiguration().getDatabaseId();
		// if (databaseId_ != null) {
		// 	databaseId_ = databaseId_.toUpperCase();
		// }
		// else {
		// 	// databaseId_ = "hsqldb";
		// 	throw new Exception("Failed to make mybatis");
		// }	
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
				if(sqlSession != null) sqlSession.close();
			}
		}

		return ret;
	}

	public int exec ( String namespace, String id, List<Object> params, int buffSize) throws DBException, Exception {

		int ret = -1;
		for( int tryCnt = 0; tryCnt < 2; ++tryCnt ) {

			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory_.openSession( ExecutorType.BATCH,  false );
				
				for ( int i = 0; i < params.size(); i++ ) {
					ret = sqlSession.update(namespace + "." + id, params.get(i));
					if ( i % buffSize == 0) { sqlSession.commit(); }
				}
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
				sqlSession.commit();
				sqlSession.close();
			}
		}
		return ret;
	}

	public int insert ( String namespace, String id, List<Object> params, int buffSize) throws DBException, Exception {

		int cnt = 0;
		for( int tryCnt = 0; tryCnt < 2; ++tryCnt ) {

			SqlSession sqlSession = null;
			try {
				sqlSession = sqlSessionFactory_.openSession( ExecutorType.BATCH,  false );
				
				for ( int i = 0; i < params.size(); i++ ) {
					sqlSession.update(namespace + "." + id, params.get(i));
					++cnt;
					if ( i % buffSize == 0) { sqlSession.commit(); }
				}
				break;
			}
			catch( Exception ex ) {
				cnt = -1;
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
				sqlSession.commit();
				sqlSession.close();
			}
		}
		return cnt;
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
		, String mybatisXml
		, String ... mappers
		) throws Exception {

		removeMybatisHandler(handlerId);

		MybatisHandler handler = new MybatisHandler(handlerId, url, userName, passwd, mybatisXml, mappers );
		instance_.put( handlerId, handler );
	}

	public static void removeMybatisHandler	(String handlerId) {
		instance_.remove(handlerId);
	}
	/*

	private static void test1(final MybatisHandler handler) {
		try {
			handler.exec( "ORACLE", "select_tab", null);
		} catch (DBException e1) {
			e1.printStackTrace();
		}
		catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	private static void test2() {
		SqlSession sqlSession = MybatisHandler.getInstance("ims").openSession(true);
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
	}

	public static void main(String[] args) {

		try {
			MybatisHandler.makeMybatisHandler	(
										"ims",
										"jdbc:oracle:thin:@210.206.96.35:1521:igov",
										"b2b_gw_dev",
										"b2b_gw_dev",
										"cfg/mybatis-config.xml",
										"cfg/mybatis-oracle.xml"
										);

			test1( MybatisHandler.getInstance("ims"));


		} catch (IOException e) {
			e.printStackTrace();
		}
	}
*/
}
