package com.hopper.dbagent;

import com.hopper.dbagent.common.IAgentDBCore;
import com.hopper.dbagent.common.IAgentTS;
import com.hopper.dbagent.excel.ExcelHandler;
import com.hopper.dbagent.vo.MessageJobVO;
import com.hopper.dbagent.vo.MessageStatsVO;
import com.hopper.dbagent.vo.MessageTodayStatsVO;
import com.hopper.dbagent.vo.MessageVO;
import hpr.que.bptree.UniqueDB;
import hpr.que.filedb.BPTree.DiskMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class AgentDBImpl implements IAgentDBCore {
	private static final Logger log = LoggerFactory.getLogger(AgentDBImpl.class);

	private MybatisHandler db = null;
	private MybatisHandler fileDB = null;

	private String DB_NAMESPACE = "";

	
	private MsgStatsHandler statsHandler;
	private UniqueDB filedb; 

	public AgentDBImpl(){
		init();
	}

	private void init() {
		try {
			filedb = new UniqueDB(30, DiskMode.DISK);
			filedb.createOpen ("", hpr.util.Config.cfg().get("app.etc.uniqueDB"));
		} 
		catch(Exception e) {
			log.warn(StackTrace.toString(e));
		}
	}


	
	public void connectDB(String mybatisConfFile, String xmlFile, String xmlNS, String dbUrl, String userName, String passwd) throws Exception {

		DB_NAMESPACE = xmlNS;
		MybatisHandler.makeMybatisHandler(
			DB_NAMESPACE,
			dbUrl,
			userName,
			passwd,
			mybatisConfFile,
			xmlFile
			);
		db = MybatisHandler.getInstance(DB_NAMESPACE);
		statsHandler = new MsgStatsHandler(db, DB_NAMESPACE);

		// test connect
		db.selectOne(DB_NAMESPACE, "select_test", null);
	}

	/**
		BR_MSG_DATA테이블을 Polling하는 함수.
		BR_MSG_DATA테이블이 존재하려면 BR_MSG_JOB테이블이 필요하기에 Exception발생시 두개의 테이블 같이 생성.
		BR_MSG_JOB테이블에 YYYYMM_SMS(LMS, MMS) 3개 데이터 기본 INSERT
		**/
		public int selectAndUpdate(IAgentTS app) {

			if (!app.isWritable()) {
				return 0;
			}
			int cnt = 0;

			try {
				Map<String, Object> where = new HashMap<String, Object>();
				where.put("status", 1);
				where.put("sendDate", new Date());
				where.put("rownum", 100);

				List<Object> rows = db.selectList(DB_NAMESPACE, "select_msg", where);

				for (Object r: rows) {
					MessageVO row = (MessageVO)r;
					log.debug("FETCH: " + row.toString());

					boolean res = false;
					if (row.msgType.equals("SMS")) {
						res = app.sendSms(row.msgKey, row.phone, row.callback, row.msgType, row.text);
					}
					else {
						res = app.sendMms(row.msgKey, row.phone, row.callback, row.msgType, row.subject, row.text, row.fileName1, row.fileType1, row.fileName2, row.fileType2, row.fileName3, row.fileType3);
					}

					if (res) {
						row.status = 2;
						row.doneDate = new Date();
						db.exec(DB_NAMESPACE, "update_msg_sent", row);

						if (row.jobKey != null) {
							db.exec(DB_NAMESPACE, "update_msg_job", new MessageJobVO(row.jobKey, 1, 0, 0));
						}

						hpr.util.Serialize seri = new hpr.util.Serialize();
						try {
							filedb.insert(row.msgKey.getBytes(), seri.marshalling(row));
						}
						finally {
							seri.close();
						}
						
						++cnt;
					}
					else {
						log.warn("Failed to send message. Check the send line");
						Thread.sleep(5000);
					}

				}
			}
			catch(DBException ex) {
				if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
					try {
						db.exec(DB_NAMESPACE, "create_msg_data_table", null);
						log.info("CREATE [BR_MSG_DATA] TABLE");
					}
					catch(Exception e) {
						log.warn(StackTrace.toString(e));
					}

					try {
						db.exec(DB_NAMESPACE, "create_msg_table_seq", null);
						log.info("CREATE [BR_MSG_DATA_SEQ] SEQUENCE");
					}
					catch(DBException dbe) {
						if (dbe.getCommonDBErrorCode() == DBException.Code.NAME_IS_ALREADY_USED) {
							log.info("[BR_MSG_DATA_SEQ] IS ALREADY USED NAME");
						}
						else {
							log.error(StackTrace.toString(dbe));
						}
					}
					catch(Exception e) {
						log.warn(StackTrace.toString(e));
					}
				}
				else {
					log.warn(StackTrace.toString(ex));
				}
			}
			catch(Exception ex) {
				log.warn(StackTrace.toString(ex));
			}
			return cnt;
		}



		public boolean updateSentFail(String msgKey, String data) {

			MessageVO row = new MessageVO();

			row.msgKey = msgKey;
			row.status = 3; // 
			row.doneDate = new Date();
			row.rsltCode = Utils.changeRsltCodeData(data);
			row.rsltData = Utils.ellipsis(data, 100);

			try {
				log.info("UPDATE SENT FAIL: " + row.toString());

				db.exec(DB_NAMESPACE, "update_msg_sent_fail", row);
				
				byte[] selectData = filedb.selectAndDelete(row.msgKey.getBytes());
				if (selectData.length > 0) {
					
					hpr.util.Serialize seri = new hpr.util.Serialize();
					try {
						MessageVO oldRow = (MessageVO)seri.unmarshalling(selectData);
						if (oldRow.jobKey != null) {
							db.exec(DB_NAMESPACE, "update_msg_job", new MessageJobVO(oldRow.jobKey, 0, 0, 1));
						}
						//sendDate, msgType, sentFail, succ, timeout, invalid
						statsHandler.update(oldRow.sendDate, oldRow.msgType, 1, 0, 0, 0);
						}
					finally {
						seri.close();
					}
				} 
				return true;
			}
			catch(Exception ex) {
				log.warn(StackTrace.toString(ex));
			}
			return false;
		}


	// msgKey : MSG KEY, 
	// date : 폰에전달받은시간,
	// code : 100, 200, 300... 
	// data : code 에 대한 데이터 값
	// net : 이통사코드 SKT, KTF, LGT ..

		public boolean updateReport(String msgKey, String date, String code, String data, String net) {
		//

			MessageVO row 	= new MessageVO();
			row.msgKey 			= msgKey;
			row.status 			= 3;
			row.reportDate 	= new Date();
			try {
				row.phoneDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(date);
			}
			catch (java.text.ParseException ex) {
				row.phoneDate = new Date();
				log.warn("Invalid date format: " + date);
			}
			row.netCode  		= Utils.changeNetCode(net);
			row.rsltCode 		= Utils.changeRsltCodeData(data);
			row.rsltData 		= Utils.ellipsis(data, 100);

			try {
				db.exec(DB_NAMESPACE, "update_msg_report", row);
				byte[] selectData = filedb.selectAndDelete(row.msgKey.getBytes());
				if (selectData.length > 0) {
					hpr.util.Serialize seri = new hpr.util.Serialize();
					try {
						MessageVO oldRow = (MessageVO)seri.unmarshalling(selectData);
				
						int succ = 0;
						int timeout = 0;
						int invalid = 0;

						if ("100".equals(code)) 			++succ;
						else if ("400".equals(code)) 	++timeout;
						else if ("410".equals(code))	++invalid;					

						statsHandler.update(oldRow.sendDate, oldRow.msgType, 0, succ, timeout, invalid );

						if (oldRow.jobKey != null) {
							db.exec(DB_NAMESPACE, "update_msg_job", new MessageJobVO(oldRow.jobKey, 0, succ, timeout + invalid));
						}
					}
					finally {
						seri.close();
					}
				}

				return true;
			}
			catch(Exception ex) {			
				log.warn(StackTrace.toString(ex));
			}

			return false;
		}


	public int selectAndInsertDelete() {
		int moveCount = 0;
		Map<String, Object> map = new HashMap<>();
		String logTableName = new SimpleDateFormat("yyyyMM").format(new Date());
		map.put("status", 3);
		map.put("rownum", 100);
		map.put("moveDate", new Date((new Date()).getTime() - 5 * 60));
		map.put("logTableName", logTableName);
		try {
			List<Object> msgList = db.selectList(DB_NAMESPACE, "select_move_msg", map);

			Map<String, Object> rowMapData = null;
			for (Object msg : msgList) {
				rowMapData = (HashMap<String, Object>) msg;
				rowMapData.put("logTableName", logTableName);
				Set<String> keySet = rowMapData.keySet(); 
				Iterator<String> iter = keySet.iterator();
				while( iter.hasNext() ) {
					String key = iter.next();
					Object value = rowMapData.get(key);
					if( value instanceof BigDecimal ) {
						BigDecimal d = (BigDecimal) value;
						rowMapData.put(key, d.intValue());						
					}
				}
				log.debug("==================================================");
				log.debug(rowMapData.toString());
				log.debug("==================================================");


				String mapKeyType = "";
				if     ("mysql".equals(DB_NAMESPACE))  { mapKeyType = "msgKey"; }
				else if("oracle".equals(DB_NAMESPACE)) { mapKeyType = "MSGKEY"; }
				else if("hsqldb".equals(DB_NAMESPACE)) { mapKeyType = "MSGKEY";}
				else   {throw new Exception("지원하지 않는 DB입니다. [" +  DB_NAMESPACE + "]");}

				try {
					db.exec(DB_NAMESPACE, "insert_msg_log", rowMapData);
					db.exec(DB_NAMESPACE, "delete_msg", rowMapData.get(mapKeyType));
					++moveCount;
				} 
				catch(DBException ex) {
					if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
						try {
							db.exec(DB_NAMESPACE, "create_msg_log_table", map);
							log.warn("CREATE [BR_MSG_DATA_LOG_" + logTableName + "] TABLE" );
						}
						catch(Exception e) {
							log.warn(e.toString());
						}
						return moveCount;
					}
					else {
						log.error(StackTrace.toString(ex));
					}
				}
				catch(Exception ex){
					log.error(StackTrace.toString(ex));
				}
			}// end for loop

		}// end for try
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
				try {
					db.exec(DB_NAMESPACE, "create_msg_data_table", null);
					log.info("CREATE [BR_MSG_DATA] TABLE");
				}
				catch(Exception e) {
					log.warn(e.toString());
				}

				try {
					db.exec(DB_NAMESPACE, "create_msg_table_seq", null);
					log.info("CREATE [BR_MSG_DATA_SEQ] SEQUENCE");
				}
				catch(DBException dbe) {
					if (dbe.getCommonDBErrorCode() == DBException.Code.NAME_IS_ALREADY_USED) {
						log.info("[BR_MSG_DATA_SEQ] IS ALREADY USED NAME");
					}
					else {
						log.info("[{}]은 SEQUENCE를 생성하지 않습니다.", DB_NAMESPACE);
						log.error(StackTrace.toString(dbe));
					}
				}
				catch(Exception e) {
					log.warn(e.toString());
				}
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception ex){
			log.error(StackTrace.toString(ex));
		}
		return moveCount;
	}




	public boolean insertMsg(String phone, String callback, String text, String msgType, String resvYN) {
		MessageVO row = new MessageVO();		
		row.phone			= phone;
		row.status 		= 1;
		row.callback	= callback;
		row.text			= text;
		row.msgType	 	= msgType;
		row.sendDate 	= new Date();
		row.resvYN    = resvYN;
		
		try {
			db.exec(DB_NAMESPACE, "insert_msg_data", row);			
			return true;
		}
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
				log.warn(StackTrace.toString(ex));
				try {
					db.exec(DB_NAMESPACE, "create_msg_data_table", null);
					log.warn("CREATE [BR_MSG_DATA] TABLE" );

				}
				catch(Exception e) {
					log.warn(StackTrace.toString(e));
				}
			}			
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception ex) {			
			log.error(StackTrace.toString(ex));
		}
		return false;
	}


	public int excelDataInsert( String excelFile ,String jobKey, String jobName ,String msgType ,String callback ,String resvYN , Date sendDate) {
		// selectAndInsertDeleteMsgTemp 작업에서 사용할 데이터를 멤버변수에 저장.
		Date nowTime = new Date();
		sendDate = (sendDate.getTime() < nowTime.getTime()) ? nowTime : sendDate;

		ExcelHandler excelHandler = new ExcelHandler(excelFile, jobKey, jobName, msgType, callback, resvYN, sendDate);
		List<Object> excelList = excelHandler.readExcelData().first();
		int cnt = -1;
		Map<String, Object> map = new HashMap<>();
		map.put("totalCnt"	,	excelList.size());
		map.put("jobKey"		,	jobKey);
		map.put("regDate"		, new Date());
		map.put("jobName"		,	jobName);
		map.put("msgType"		,	msgType);
		map.put("callback"	, callback);
		map.put("sendDate"	, sendDate);
		map.put("resvYN"		,	resvYN);

		for (int i=0; i < 2; ++i) {
			try {
				db.exec(DB_NAMESPACE, "insert_msg_job", map);
				break;
			}
			catch(DBException ex) {
				if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
					try {
						db.exec(DB_NAMESPACE, "create_msg_job_table", null);
						log.info("CREATE [BR_MSG_JOB] TABLE");
					}
					catch(Exception exce) {
						log.warn(exce.toString());
						return cnt;
					}
				}
				else {
					log.warn(StackTrace.toString(ex));
					return cnt;
				}
			}
			catch(Exception ex) {
				log.warn(StackTrace.toString(ex));
				return cnt;
			}
		}
		

		for (int i=0; i < 2; ++i) {
			try {
				cnt = db.insert(DB_NAMESPACE, "insert_msg_data", excelList, 100);
				log.info("MSG_TEMP Data Size : {}", excelList.size());
				return cnt;
			}
			catch(DBException ex) {
				if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
					try {
						db.exec(DB_NAMESPACE, "create_msg_data_table", null);					
						log.info("CREATE [BR_MSG_DATA] TABLE");
					}
					catch(Exception exce) {
						log.warn(exce.toString());
					}
				}
				else {
					log.warn(StackTrace.toString(ex));
					return cnt;
				}
			}
			catch(Exception ex) {
				log.warn(StackTrace.toString(ex));
				return cnt;
			}
		}
		return cnt;
	}


	public hpr.util.Pair<List<Object>,List<Object>> excelDataTest (String excelFile, String jobName ,String msgType ,String callback ,String resvYN , Date sendDate) {
		
		ExcelHandler excelHandler = new ExcelHandler(excelFile, jobName, jobName, msgType, callback, resvYN, sendDate);

		return excelHandler.readExcelData();
	}

	public int selectJobListCnt(String beginSendDate
															  ,String endSendDate
															  ,String callback
															  ,String jobKey
															  ,String msgType
															  ,String resvYN) {

		int totalCount = 0;
		try {
			Map<String, Object> where = new HashMap<String, Object>();
			where.put("beginSendDate",  beginSendDate);	
			where.put("endSendDate"  ,	endSendDate);
			where.put("callback", callback);
			where.put("jobKey", jobKey);
			where.put("msgType", msgType);
			where.put("resvYN", resvYN);

			totalCount = (int) db.selectOne(DB_NAMESPACE, "select_msg_job_total_count", where);
		} 
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
				return 0;
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception e){
			log.error(StackTrace.toString(e));
		}
		return totalCount;
	}



	public List<MessageJobVO> selectJobList(int pageNo
																				 ,int offset
																				 ,int limit
																				 ,String beginSendDate
																				 ,String endSendDate
																				 ,String callback
																				 ,String jobKey
																				 ,String msgType
																				 ,String resvYN
																				 )
	{
		List<MessageJobVO> jobList = new ArrayList<>();
		List<Object> obj = null;
		try {
			Map<String, Object> where = new HashMap<String, Object>();
			where.put("pageNo" , pageNo);
			where.put("offset" , offset);
			where.put("limit" , limit);
			where.put("beginSendDate",  beginSendDate);	
			where.put("endSendDate"  ,	endSendDate);
			where.put("callback", callback);
			where.put("jobKey", jobKey);
			where.put("msgType", msgType);
			where.put("resvYN", resvYN);

			obj = db.selectList(DB_NAMESPACE, "select_msg_job", where);
			for (Object e : obj) {
				MessageJobVO jobVO = (MessageJobVO) e;
				jobList.add(jobVO);
			}
		}
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
				return jobList;
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception e) {
			log.error(StackTrace.toString(e));
		}

		return jobList;
	}

	/**
	  엑셀 메세지 조회 
	  msgKey 앞 6자리로 yyyymm 구분
	**/
	public int selectMsgJobDataListCnt(String jobKey, String phone, String rsltCode, String status){
		int totalCount = 0;
		try {
			Map<String, Object> where = new HashMap<String, Object>();			
			where.put("jobKey", jobKey);
			where.put("phone", phone);
			where.put("rsltCode", rsltCode);
			where.put("yyyymm", jobKey.substring(7, 13));
			where.put("status", status);	
  	

			totalCount = (int) db.selectOne(DB_NAMESPACE, "select_msg_job_data_list_cnt", where);
		}
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
				return 0;
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception e){
			log.error(StackTrace.toString(e));
		}
		return totalCount;
	}


  public List<MessageVO> selectMsgJobDataList( int pageNo
  																						,int offset
                                              ,int limit
                                              ,String jobKey
                                              ,String phone
                                              ,String rsltCode
                                              ,String status) {
  	List<MessageVO> list = new ArrayList<>();
		List<Object> obj = null;
		try {
			Map<String, Object> where = new HashMap<String, Object>();
			where.put("pageNo" , pageNo);
			where.put("offset" , offset);
			where.put("limit" , limit);
			where.put("jobKey", jobKey);
			where.put("phone", phone);
			where.put("rsltCode", rsltCode);
			where.put("yyyymm", jobKey.substring(7, 13));
			where.put("status", status);
  		

			obj = db.selectList(DB_NAMESPACE, "select_msg_job_data_list", where);
			for (Object e : obj) {
				MessageVO msgVO = (MessageVO) e;
				list.add(msgVO);
			}
			log.info(list.toString());
		}

		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
				return list;
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception e) {
			log.error(e.getMessage());
		}
  	return list;
  }

  /**
	일반 메세지 + 엑셀전송 메세지 조회
	yyyymm -> beginSendDate, endSendDate중 하나에 yyyymm 추출해서 사용.
	★★★필수파라미터★★★ -> beginSendDate, endSendDate
  **/
  public int selectMsgDataListCnt( String beginSendDate 
  																,String endSendDate
  																,String phone
  																,String callback
  																,String msgKey
  																,String msgType
  																,String resvYN
  																,String status 
  																,String rsltCode )
  {
  	int totalCount = 0;
		try {
			Map<String, Object> where = new HashMap<String, Object>();
			where.put("beginSendDate", beginSendDate);
			where.put("endSendDate",	 endSendDate);
  		where.put("phone", 				 phone);
  		where.put("callback", 		 callback);
  		where.put("msgKey", 			 msgKey);
  		where.put("msgType", 			 msgType);
  		where.put("resvYN", 			 resvYN);
  		where.put("rsltCode",      rsltCode);
  		where.put("status",        status);	

  		if(!beginSendDate.isEmpty() && beginSendDate != null) {
  			where.put("yyyymm", beginSendDate.substring(0, 6));
  		} else {
  			where.put("yyyymm", new SimpleDateFormat("yyyyMM").format(new Date()));
  		}

			totalCount = (int) db.selectOne(DB_NAMESPACE, "select_msg_data_list_cnt", where);
		}
		catch(DBException ex) {
			if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
				return 0;
			}
			else {
				log.warn(StackTrace.toString(ex));
			}
		}
		catch(Exception e){
			log.error(StackTrace.toString(e));
		}
		return totalCount;
  }


  public List<MessageVO> selectMsgDataList(int pageNo
  																				,int offset
	                                        ,int limit
	                                        ,String beginSendDate 
  																        ,String endSendDate
	                                        ,String phone
	                                        ,String callback
	                                        ,String msgKey
	                                        ,String msgType
	                                        ,String resvYN
	                                        ,String status
	                                        ,String rsltCode)
  {
  	List<MessageVO> list = new ArrayList<>();
  	List<Object> obj = null;
  	try {
  		Map<String, Object> where = new HashMap<String, Object>();
  		where.put("pageNo", 				pageNo);
  		where.put("offset", 				offset);
  		where.put("limit" , 				limit);
  		where.put("beginSendDate", 	beginSendDate);
			where.put("endSendDate", 		endSendDate);
  		where.put("phone", 					phone);
  		where.put("callback", 			callback);
  		where.put("msgKey", 				msgKey);
  		where.put("msgType", 				msgType);
  		where.put("resvYN", 				resvYN);
  		where.put("status", 				status);	
  		where.put("rsltCode", 			rsltCode);
  		log.info(where.toString());
  		if(!beginSendDate.isEmpty() && beginSendDate != null) {
  			where.put("yyyymm", beginSendDate.substring(0, 6));
  		} else {
  			where.put("yyyymm", new SimpleDateFormat("yyyyMM").format(new Date()));
  		}
  		
  		obj = db.selectList(DB_NAMESPACE, "select_msg_data_list", where);
  		for (Object e : obj) {
  			MessageVO msgVO = (MessageVO) e;
  			list.add(msgVO);
  		}
  	}
  	catch(DBException ex) {
  		if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
  			return list;
  		}
  		else {
  			log.warn(StackTrace.toString(ex));
  		}
  	}
  	catch(Exception e) {
  		log.warn(StackTrace.toString(e));
  	}
  	return list;
  }





  /**
	일일 SMS, LMS, MMS 통계 리스트
  **/

	public List<MessageStatsVO> selectMsgStatsList(String statsType, String yyyy, String mm, String msgType, String sentRslt) {
		List<MessageStatsVO> rsltList = new ArrayList<>();
		List<Object> objList = new ArrayList<>();
		Map<String, Object> where = new HashMap<String, Object>();
		where.put("yyyy", yyyy);
		where.put("mm", mm);
		where.put("msgType", msgType);
		where.put("sentRslt", sentRslt);
		String statsUp = statsType.toUpperCase();
		String selectId = "";
		log.debug("STATS TYPE : " + statsType.toUpperCase());
		try {
			if("Y".equals(statsUp))  			 selectId = "select_msg_stats_year_list";
			else if ("M".equals(statsUp))  selectId = "select_msg_stats_month_list";
			else 													 selectId = "select_msg_stats_day_list"; // stats = D

			objList = db.selectList(DB_NAMESPACE, selectId, where);
			if(!objList.isEmpty() && objList != null) {
				for(Object vo : objList) {
					MessageStatsVO statsVO = (MessageStatsVO) vo;
					rsltList.add(statsVO);
				}	
			}
		}
		catch(DBException ex) {
  		if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
  			log.debug(StackTrace.toString(ex));
  			log.info("Stats table does not exist");
  		}
  		else {
  			log.warn(StackTrace.toString(ex));
  		}
  	}
		catch(Exception e) {
			log.error(StackTrace.toString(e));
		}
		return rsltList;
  }


  /**
	Main 통계
  **/

  public List<MessageTodayStatsVO> selectMsgDataTodayReady() {
  	List<MessageTodayStatsVO> todayReadyList = new ArrayList<MessageTodayStatsVO>();
  	List<Object> obj = null;
  	try {
  		Map<String, Object> where = new HashMap<String, Object>();
  		Date now = new Date();
  		where.put("now", now);
  		obj = db.selectList(DB_NAMESPACE, "select_msg_data_today_ready", where);

  		if (obj != null) {
  			for(Object vo : obj) {
  				MessageTodayStatsVO todayStatsVO = (MessageTodayStatsVO) vo;
  				todayReadyList.add(todayStatsVO);
  			}
  		}
  	} 
  	catch(DBException ex) {
  		if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
  			log.debug(StackTrace.toString(ex));
  			log.info("Stats table does not exist");
  		}
  		else {
  			log.warn(StackTrace.toString(ex));
  		}
  	}
  	catch(Exception e){
  		log.error(StackTrace.toString(e));
  	}
  	return todayReadyList;
  }


  public List<MessageTodayStatsVO> selectMsgDataTodaySuccFail() {
  	List<MessageTodayStatsVO> succList = new ArrayList<MessageTodayStatsVO>();
  	List<Object> obj = null;
  	try {
  		Map<String, Object> where = new HashMap<String, Object>();
  		Date now = new Date();
  		where.put("yyyymm", new SimpleDateFormat("yyyyMM").format(now));
  		where.put("now", now);

  		obj = db.selectList(DB_NAMESPACE, "select_msg_data_today_succ_fail", where);
  		if (obj != null) {
  			for(Object vo : obj) {
  				MessageTodayStatsVO todayStatsVO = (MessageTodayStatsVO) vo;
  				if(todayStatsVO == null) log.info(todayStatsVO.toString());
  				succList.add(todayStatsVO);
  			}
  		}
  	} 
  	catch(DBException ex) {
  		if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {				
  			log.debug(StackTrace.toString(ex));
  			log.info("Stats table does not exist");
  			return succList;
  		}
  		else {
  			log.warn(StackTrace.toString(ex));
  		}
  	}
  	catch(Exception e){
  		log.error(StackTrace.toString(e));
  	}
  	return succList;
  }




	





























	private String createJobKey(){
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + (new Random().nextInt(999999));
	}

	public void createAllTable(){
		try {
			db.exec(DB_NAMESPACE, "create_msg_job_table", null);
			log.warn("BR_MSG_JOB 테이블 생성");
		} catch(Exception e) {
			log.warn("BR_MSG_JOB 테이블이 존재합니다.");
		}
		

		try {
			db.exec(DB_NAMESPACE, "create_msg_data_table", null);
			log.warn("BR_MSG_DATA 테이블 생성");
		} catch(Exception e) {
			log.warn("BR_MSG_DATA 테이블이 존재합니다.");
		}

		try {
			db.exec(DB_NAMESPACE, "create_msg_temp_table", null);
			log.warn("BR_MSG_TEMP 테이블 생성");
		} catch(Exception e) {
			log.warn("BR_MSG_TEMP 테이블이 존재합니다.");
		}
	}





/*
Message 본문 입력 시 줄 바꿈을 입력하기 위해서는 DB별로 아래처럼 처리하시면 됩니다. 

▶ Oracle의 경우
'안녕하세요'||chr(13)||chr(10)||'좋은 하루 되십시오'

▶ MSSQL의 경우
'안녕하세요'+char(13) + char(10) + '좋은 하루 되십시오'

▶ MySQL의 경우
'안녕하세요\r\n좋은 하루 되십시오'
	*/




	public static void main(String[] args) {
		
		// try {
		// 	long start=0, end = 0;
		// 	AgentDBImpl agent = new AgentDBImpl();
			
		// 	agent.connectDB("cfg/mybatis-config.xml", 
		// 		"cfg/mybatis-oracle-dbagent.xml", 
		// 		"oracle",
		// 		"jdbc:oracle:thin:@210.206.96.35:1521:igov",
		// 		"b2b_gw_dev",
		// 		"b2b_gw_dev"
		// 		);



		// 	while(true) {
		// 		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		// 		log.info("===============================");
		// 		log.info("1. BR_MSG_DATA Insert Data 20건");
		// 		log.info("2. BR_MSG_TEMP ExcelData 10만건 Insert");
		// 		log.info("3. BR_MSG_TEMP ExcelData 1000건 Insert");
		// 		log.info("4. BR_MSG_TEMP ExcelData -> BR_MSG_DATA 이동 ");
		// 		log.info("5. BR_MSG_DATA -> BR_MSG_LOG_YYYYMM 이동 ");
		// 		log.info("6. CREATE TABLE ALL");
		// 		log.info("7. FILE TEMP TABLE SIZE");
		// 		log.info("8. SELECT FILE TEMP");
		// 		log.info("===============================");
		// 		String readLine = br.readLine();
		// 		switch(readLine) {
		// 			case "1" :
		// 			start = System.currentTimeMillis();
		// 			agent.insertMsg("01112345600", "010-5099-5149", "예상 RSLTCODE : 0", "SMS", "N");
		// 			agent.insertMsg("01112345601", "010-5099-5149", "예상 RSLTCODE : 1", "SMS", "N");
		// 			agent.insertMsg("01112345602", "010-5099-5149", "예상 RSLTCODE : 5", "SMS", "N");
		// 			agent.insertMsg("01112345603", "010-5099-5149", "예상 RSLTCODE : 6", "SMS", "N");
		// 			agent.insertMsg("01112345604", "010-5099-5149", "예상 RSLTCODE : 7", "SMS", "N");
		// 			agent.insertMsg("01112345605", "010-5099-5149", "예상 RSLTCODE : 8", "SMS", "N");
		// 			agent.insertMsg("01112345606", "010-5099-5149", "예상 RSLTCODE : 9", "SMS", "N");
		// 			agent.insertMsg("01112345607", "010-5099-5149", "예상 RSLTCODE : A", "SMS", "N");
		// 			agent.insertMsg("01112345608", "010-5099-5149", "예상 RSLTCODE : E", "SMS", "N");
		// 			agent.insertMsg("01112345609", "010-5099-5149", "예상 RSLTCODE : B", "SMS", "N");
		// 			agent.insertMsg("01112345610", "010-5099-5149", "예상 RSLTCODE : C", "SMS", "N");
		// 			agent.insertMsg("01112345611", "010-5099-5149", "예상 RSLTCODE : D", "SMS", "N");
		// 			agent.insertMsg("01112345612", "010-5099-5149", "예상 RSLTCODE : F", "SMS", "N");
		// 			agent.insertMsg("01112345613", "010-5099-5149", "예상 RSLTCODE : I", "SMS", "N");
		// 			agent.insertMsg("01112345614", "010-5099-5149", "예상 RSLTCODE : J", "SMS", "N");
		// 			agent.insertMsg("01112345615", "010-5099-5149", "예상 RSLTCODE : K", "SMS", "N");
		// 			agent.insertMsg("01112345616", "010-5099-5149", "예상 RSLTCODE : L", "SMS", "N");
		// 			agent.insertMsg("01112345617", "010-5099-5149", "예상 RSLTCODE : M", "SMS", "N");
		// 			agent.insertMsg("01112345618", "010-5099-5149", "예상 RSLTCODE : S", "SMS", "N");
		// 			agent.insertMsg("01112345619", "010-5099-5149", "예상 RSLTCODE : 2", "SMS", "N");
		// 			break;
					
		// 			case "2" :
		// 			start = System.currentTimeMillis();
		// 			// agent.excelDataInsertToMsgTemp("excel\\xlsx-100000-test.xlsx", "테스트 잡 이름", "SMS", "010-5099-5149", new Date());
		// 			break;
					
		// 			case "3" :
		// 			start = System.currentTimeMillis();
		// 			// agent.excelDataInsertToMsgTemp("excel\\xlsx-1000-test.xlsx", "테스트 잡 이름", "SMS", "010-5099-5149", new Date());
		// 			break;
					
		// 			case "4" :
		// 			start = System.currentTimeMillis();
		// 			String jobKey = "";//agent.selectAndInsertDeleteMsgTemp();
		// 			log.info("JobKey : " + jobKey);
		// 			break;
					
		// 			case "5" :
		// 			start = System.currentTimeMillis();
		// 			int moveCount = agent.selectAndInsertDelete();
		// 			log.info("MOVE_MSG_DATA : " + moveCount);
		// 			break;

		// 			case "6" :
		// 			start = System.currentTimeMillis();
		// 			agent.createAllTable(); 
		// 			break;

		// 			case "7" :
		// 			start = System.currentTimeMillis();
					
		// 			break;

		// 			case "8" :
		// 			start = System.currentTimeMillis();
					
		// 			break;

		// 			default :
		// 			System.exit(-1);
		// 		}

		// 		end = System.currentTimeMillis();
		// 		log.info((end - start) / (1000 * 60)+ "분 걸림.");
		// 		log.info((end - start) / 1000 + "초 걸림.");
		// 		log.info((end - start) + "밀리세컨 걸림.");
		// 	}
		// } 
		// catch (Exception e) {
		// 	e.printStackTrace();
		// }
	}


}
