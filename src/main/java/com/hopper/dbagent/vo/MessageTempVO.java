package com.hopper.dbagent.vo;
import com.hopper.dbagent.Utils;
import java.util.Date;

public class MessageTempVO {

    public long tempNo;
    public int status;
    public String sendFlag;
    public String jobKey;
    public String jobName;
    public String phone;
    public String callback;
    public String msgType;
    public String subject;
    public String text;
    public Date sendDate;
    public String resvYN;
    public String fileName1;
    public String fileType1;
    public String fileName2;
    public String fileType2;
    public String fileName3;
    public String fileType3;


    public String jobNameErr;
    public String phoneErr;
    public String callbackErr;
    public String msgTypeErr;
    public String textErr;

    public MessageTempVO(){
        
    }

    public MessageTempVO (String jobKey, String jobName, String msgType, String callback, String resvYN, Date sendDate) {
        this.jobKey = jobKey;
        this.jobName = jobName;
        this.msgType = msgType;
        this.callback = callback;
        this.sendDate = sendDate;
        this.resvYN   = resvYN;
        this.status = 1;
    }


    @Override
    public String toString() {
        return  "TEMP_NO : " + tempNo +", SEND_FLAG : " + sendFlag + ", JOB_NAME : " + jobName + ", PHONE :" + phone + 
             	  ", CALLBACK : " + callback + ", MSG_TYPE : " + msgType + ", SUBJECT : " + subject + ", SEND_DATE : " + sendDate + ", RESV_YN : " + resvYN + 
                ", TEXT : " + Utils.ellipsis(text, 10) + ", FILE_NAMES : " + "[" + fileName1 + "," + fileName2 + "," + fileName3 + "]";
    }	 
}