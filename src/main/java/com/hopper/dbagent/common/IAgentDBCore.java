package com.hopper.dbagent.common;

import java.lang.Exception;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.hopper.dbagent.vo.MessageVO;
import com.hopper.dbagent.vo.MessageJobVO;
import com.hopper.dbagent.vo.MessageStatsVO;
import com.hopper.dbagent.vo.MessageTodayStatsVO;


public interface IAgentDBCore extends IAgentDB {
  public void connectDB(String mybatisConfFile, String xmlFile, String xmlNS, String dbUrl, String userName, String passwd) throws Exception;
  public int selectAndUpdate(IAgentTS app);
  public int selectAndInsertDelete();

  public boolean insertMsg(String phone, String callback, String text, String msgType, String resvYN);

  public int excelDataInsert(String excelFile, String jobKey, String jobName, String msgType, String callback, String resvYN, Date sendDate);
  public hpr.util.Pair<List<Object>,List<Object>> excelDataTest (String excelFile, String jobName ,String msgType ,String callback ,String resvYN , Date sendDate);

  public List<MessageJobVO> selectJobList(int pageNo
                                         ,int offset
																				 ,int limit
																				 ,String beginSendDate
																				 ,String endSendDate
																				 ,String callback
																				 ,String jobKey
																				 ,String msgType
																				 ,String resvYN
																				 );
  public int selectJobListCnt(String beginSendDate
                                ,String endSendDate
                                ,String callback
                                ,String jobKey
                                ,String msgType
                                ,String resvYN);



  public int selectMsgJobDataListCnt(String jobKey, String phone, String rsltCode, String status);
  public List<MessageVO> selectMsgJobDataList(int pageNo
                                               ,int offset
                                               ,int limit
                                               ,String jobKey
                                               ,String phone
                                               ,String rsltCode
                                               ,String status);



public int selectMsgDataListCnt( String beginSendDate 
                                ,String endSendDate
                                ,String phone
                                ,String callback
                                ,String msgKey
                                ,String msgType
                                ,String resvYN 
                                ,String status 
                                ,String rsltCode);

 public List<MessageVO> selectMsgDataList( int pageNo
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
                                          ,String rsltCode);


  public List<MessageStatsVO> selectMsgStatsList(String statsType, String yyyy, String mm, String msgType, String sentRslt);


  public List<MessageTodayStatsVO> selectMsgDataTodayReady();
  public List<MessageTodayStatsVO> selectMsgDataTodaySuccFail();

}

