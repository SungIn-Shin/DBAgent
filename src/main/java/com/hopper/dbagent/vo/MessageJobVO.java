package com.hopper.dbagent.vo;
import java.util.Date;
import java.io.Serializable;
import com.hopper.dbagent.common.CommonsVO;

public class MessageJobVO  extends CommonsVO implements Serializable {

	public String jobKey;		// Job Key
	public String jobName;	// Job Name
	public String callback; // 회신번호(발신번호)
	public Date regDate;		// 등록일시
	public Date sendDate;		// 전송일시
	public String resvYN;		// Reservation 예약 여부 (Y : 예약 전송, N : 즉시 전송)
	public String msgType;	// MSG TYPE (SMS, LMS, MMS)
	public int totalCnt;		// 메세지 토탈 건수
	public int sendCnt;			// 전송 건수
	public int rsltSuccCnt;	// 전송 성공 건수
	public int rsltFailCnt;	// 전송 실패 건수
	

	public MessageJobVO() {}
	public MessageJobVO(String jobKey, int sendCnt, int rsltSuccCnt, int rsltFailCnt) {
		this.jobKey = jobKey;
		this.sendCnt = sendCnt;
		this.rsltSuccCnt = rsltSuccCnt;
		this.rsltFailCnt = rsltFailCnt;
	}

	@Override
	public String toString() {
		return "";
	}

}


										// CREATE TABLE BR_MSG_JOB
										// 			(
										// 			 JOB_KEY                  VARCHAR2(20)  NOT NULL  -- 잡 키
										// 			,JOB_NAME               	VARCHAR2(200) NOT NULL  -- 잡 이름
										// 			,CALLBACK 								VARCHAR2(20)  NOT NULL  -- 회신번호(발신번호)
										// 			,REG_DATE              		DATE          NOT NULL  -- 등록시간
										// 			,SEND_DATE                DATE          NOT NULL  -- 메세지 전송시간 (즉시전송, 예약전송)
										// 			,RESV_YN 									CHAR(1) 			NOT NULL  -- 예약전송 여부 (Y : 예약전송, N : 즉시전송)
										// 			,MSG_TYPE                	VARCHAR2(3)   NOT NULL  -- 메세지 타입(SMS, LMS, MMS)
										// 			,TOTAL_CNT          			NUMBER(10)    NOT NULL  -- 총 메세지 수
										// 			,STATE 										NUMBER(1)     NOT NULL  -- Job 상태 (완료 : 0, 전송중 : 1, 전송취소 : 2, 예약대기 : 3, 예약취소 : 4)
										// 			,SEND_CNT									NUMBER(10)		DEFAULT 0 -- 전송중 메세지
										// 			,RSLT_SUCC_CNT    				NUMBER(10)    DEFAULT 0 -- REPORT SUCCESS
										// 			,RSLT_FAIL_CNT           	NUMBER(10)    DEFAULT 0 -- REPORT FAIL or SENT FAIL

										// 			,CONSTRAINT MSG_JOB_PK primary key(JOB_KEY)
										// 			)