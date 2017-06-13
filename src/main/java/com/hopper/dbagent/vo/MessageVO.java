package com.hopper.dbagent.vo;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import com.hopper.dbagent.Utils;
import com.hopper.dbagent.common.CommonsVO;
import java.io.Serializable;

public class MessageVO extends CommonsVO implements Serializable {
	public String msgKey;
	public String jobKey;
	public String callback;
	public String phone;
	public String text;
	public String msgType;
	public int status;
	public Date sendDate;
	public String resvYN;
	public Date doneDate;
	public Date phoneDate;
	public Date reportDate;
	public String rsltCode;
	public String rsltData;
	public String netCode;
	public String subject;
	public String fileName1;
	public String fileType1;
	public String fileName2;
	public String fileType2;
	public String fileName3;
	public String fileType3;
	public String sendCancelFlag;
	public String sendCancelDate;

	public long tempNo;
	public String sendFlag;
	public String jobName;

	public MessageVO () {

	}

	public MessageVO (String jobKey, String jobName, String msgType, String callback, String resvYN, Date sendDate) {
    this.jobKey = jobKey;
    this.jobName = jobName;
    this.msgType = msgType;
    this.callback = callback;
    this.sendDate = sendDate;
    this.resvYN   = resvYN;
    this.status   = 1;
  }
	
	@Override
	public String toString() {
		return "MessageVO [msgKey=" + msgKey + ", jobKey=" + jobKey +", phone=" + phone 
				+ ", callback=" + callback + ", text=" + Utils.ellipsis(text, 10)
				+ ", sendDate=" + sendDate + ", resvYN=" + resvYN +", doneDate=" + doneDate
				+ ", phoneDate=" + phoneDate + ", reportDate=" + reportDate
				+ ", rsltCode=" + rsltCode + ", rsltData=" + rsltData + ", netCode=" + netCode
				+ ", msgType=" + msgType + ", status=" + status + ", subject=" + subject + "]"
				+ ", sendCancelFlag=" + sendCancelFlag + ", sendCancelDate=" + sendCancelDate;				
	}

}
