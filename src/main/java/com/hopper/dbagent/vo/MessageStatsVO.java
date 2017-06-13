package com.hopper.dbagent.vo;
import com.hopper.dbagent.Utils;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MessageStatsVO {

	public String yyyy;
	public String mm;
	public String dd;
	public String msgType;
	public int sentFailCnt;
	public int succCnt;
	public int toutFailCnt;
	public int invaFailCnt;
	public int totalCnt;


	public MessageStatsVO(){
		
	}

	public MessageStatsVO(Date sendDate, String msgType, int sentFail, int succ, int timeout, int invalid) {
	 	String dateFmt = new SimpleDateFormat("yyyyMMdd").format(sendDate);

		this.yyyy = dateFmt.substring(0, 4);
		this.mm 	= dateFmt.substring(4, 6);
		this.dd 	= dateFmt.substring(6, 8);

		this.msgType = msgType;
		this.sentFailCnt = sentFail;
		this.succCnt = succ;
		this.toutFailCnt = timeout;
		this.invaFailCnt = invalid;
		this.totalCnt = sentFail + succ + timeout + invalid;
	}


	@Override
	public String toString() {
		return  "YYYYMMDD : " + yyyy + mm + dd + ", " + "MSG_TYPE : " + msgType + ", " 
					+ "SUCC_CNT : " + succCnt  + ", " + "TOUT_FAIL_CNT : " + toutFailCnt + ", " + "INVA_FAIL_CNT : " + invaFailCnt
					+ ", " + "SENT_FAIL_CNT : " + sentFailCnt + ", " + "TOTAL_CNT : " + totalCnt;
	}	 

}
