package com.hopper.dbagent;

import java.sql.SQLException;
import org.apache.ibatis.executor.BatchExecutorException;

@SuppressWarnings("serial")
public class DBException extends Exception {
	public static enum Code { SUCCESS, CONNECTION_FAIL, TABLE_NOT_EXIST, DUPLICATED_ROW, UNKOWN_ERROR , NAME_IS_ALREADY_USED};
	private static enum DB { ORACLE, MSSQL, MYSQL, CUBRID ,HSQLDB };

	private DB dbType_ = null;

	private int jdbcErrorCode_;
	private String jdbcErrorStr_;



	private DBException( String dbType, int jdbcErrorCode, String jdbcErrorStr) throws Exception {
		super();

		for( int i=0; i < DB.values().length; ++i ) {
			if( dbType.equalsIgnoreCase(DB.values()[i].name())) {
				dbType_ = DB.values()[i];
			}
		}

		if( null == dbType_ ) {
			throw new Exception("Invalid dbType: " + dbType);
		}

		jdbcErrorCode_ = jdbcErrorCode;
		jdbcErrorStr_ = jdbcErrorStr.trim();

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(getCommonDBErrorCode()).append(" ");
		sb.append("[").append(getJDBCErrorCode()).append(",");
		sb.append(getJDBCErrorStr()).append("]");

		return sb.toString();
	}

	public int getJDBCErrorCode() {
		return jdbcErrorCode_;
	}

	public String getJDBCErrorStr() {
		return jdbcErrorStr_;
	}

	public Code getCommonDBErrorCode() {

		if( dbType_ == DB.ORACLE) {
			switch( jdbcErrorCode_ ) {
			case 17002:
			case 17447:
				return Code.CONNECTION_FAIL;
			case 903:
			case 942:
				return Code.TABLE_NOT_EXIST;
			case 955:
				return Code.NAME_IS_ALREADY_USED;
			case 1:
				return Code.DUPLICATED_ROW;

			default:
				return Code.UNKOWN_ERROR;
			}
		}
		else if( dbType_ == DB.MSSQL) {
			switch( jdbcErrorCode_ ) {
			case 4060:
				return Code.CONNECTION_FAIL;
//			case 903:
//				return Code.TABLE_NOT_EXIST;
			case 12601:
			case 2627:
				return Code.DUPLICATED_ROW;
			default:
				return Code.UNKOWN_ERROR;
			}
		}
		else if( dbType_ == DB.MYSQL) {
			switch( jdbcErrorCode_ ) {
			case 1:
				return Code.CONNECTION_FAIL;
			case 1146:
				return Code.TABLE_NOT_EXIST;
			case 1063:
				return Code.DUPLICATED_ROW;
			default:
				return Code.UNKOWN_ERROR;
			}
		}
		else if( dbType_ == DB.CUBRID) {
			switch( jdbcErrorCode_ ) {
			case -669:
			case -673:
				return Code.CONNECTION_FAIL;
			case -493:
				return Code.TABLE_NOT_EXIST;
			case -607:
				return Code.DUPLICATED_ROW;
			default:
				return Code.UNKOWN_ERROR;
			}
		}
		else if ( dbType_ == DB.HSQLDB ) {
			switch ( jdbcErrorCode_ ){
				case 1:
				return Code.CONNECTION_FAIL;
				case -22:
				case -5501:
				return Code.TABLE_NOT_EXIST;
				case -104:
				return Code.DUPLICATED_ROW;
				default : 
				return Code.UNKOWN_ERROR;
			}
		}
		else {
			return Code.UNKOWN_ERROR;
		}
	}

	public static void throwDBException( String dbType, Exception ex ) throws DBException, Exception {

		if( ex.getCause() instanceof SQLException ) {
			SQLException sqlEx = (SQLException) ex.getCause();
			throw new DBException( dbType, sqlEx.getErrorCode(), sqlEx.getMessage() );
		}
		else if( ex.getCause() instanceof BatchExecutorException ) {
			BatchExecutorException batchEx = (BatchExecutorException) ex.getCause();
			throw new DBException( dbType, batchEx.getBatchUpdateException().getErrorCode(), batchEx.getMessage() );
		} else {
			throw ex;	
		}		
	}


	public static void main(String[] args) {
		try {
			try {
				throw new SQLException(new SQLException("i don't know", "What? SQLState", 942));
			}
			catch( Exception ex) {
				throwDBException( "oracle", ex );
				// throwDBException( "ORACLE", ex );
			}
		}
		catch( DBException ex ) {
			System.out.println("J:" + ex.getJDBCErrorCode());
			System.out.println("M:" + ex.getJDBCErrorStr());
			System.out.println("C:" + ex.getCommonDBErrorCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
