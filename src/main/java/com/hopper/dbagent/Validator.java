package com.hopper.dbagent;

import com.hopper.dbagent.vo.MessageTempVO;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Validator{
	private static final Logger log = LoggerFactory.getLogger(Validator.class);

	public static void msgTempValidationCheck(MessageTempVO msgVO) {
		msgVO.sendFlag = "Y";

			// jobName validation
		String jobName = msgVO.jobName;
		if( jobName != null ){
			int jobNameLen = jobName.getBytes().length;
			if( jobName.isEmpty() || jobNameLen > 100 ) {
				msgVO.jobNameErr = "작업 명은 100byte 이내로 작성되어야 합니다.";
				msgVO.sendFlag = "N";
			}
		}

		// Phone validation
		String phone = msgVO.phone;
		if(phone != null) {
			int phoneLen = phone.getBytes().length;

			if( phone.isEmpty() || phone.equals("") || !(phoneLen == 10 || phoneLen == 11) ) {
				msgVO.phoneErr = "수신번호는 특수문자를 제외한 10~11글자 형식으로 작성되어야 합니다. ex) 01050995149";
				msgVO.sendFlag = "N";
			}
		}
		

		// Phone validation
		String callback = msgVO.callback;
		if(callback != null) {
			int callbackLen = callback.getBytes().length;		

			if( callback.isEmpty() || callback.equals("") || !(callbackLen == 10 || callbackLen == 11) ) {
				msgVO.callbackErr = "발신번호는 특수문자를 제외한 10~11글자 형식으로 작성되어야 합니다. ex) 01050995149";
				msgVO.sendFlag = "N";
			}			
		}


		// msgType validation  ==  SMS, LMS, MMS
		String msgType = msgVO.msgType.toUpperCase();
		if(msgType != null) {
			if( msgType.isEmpty() ||  !("SMS".equals(msgType) || "LMS".equals(msgType) || "MMS".equals(msgType)) ) {
				msgVO.msgTypeErr = "메세지 타입은 [SMS, LMS, MMS] 형식만 지원합니다.";
				msgVO.sendFlag = "N";
			}
		}


		String text = msgVO.text;
		if(text != null) {
			int textLen = text.getBytes().length;
			if( "SMS".equals(msgType) && textLen > 90 ) {
				msgVO.textErr = "SMS의 본문 크기는 90byte까지 지원합니다.";
				msgVO.sendFlag = "N";
			} else if( "LMS".equals(msgType) && textLen > 1500 ) {
				msgVO.textErr = "LMS의 본문 크기는 1500byte까지 지원합니다.";
 				msgVO.sendFlag = "N";
			}
		}
		

}

}