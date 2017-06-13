package com.hopper.dbagent;

import java.util.UUID;
public class Utils {
	static public String ellipsis(String text, int length) {

		String ellipsisString = "...";
		String outputString = text;

		if(text != null && text.length() > 0 && length > ellipsisString.length()){
			if(text.length() > length + ellipsisString.length()){
				outputString = text.substring(0, length - ellipsisString.length());
				outputString += ellipsisString;
			}
		}
		return outputString;
	}

	public static String createUniqueKey(){
		return UUID.randomUUID().toString().replace("-", "").substring(0, 30);
	}

	public static String changeNetCode(String net){
		String rslt = "";
		if (net.equals("SKT")) {
			rslt = "S";
		} 
		else if (net.equals("KTF")) {
			rslt = "K";
		} 
		else if (net.equals("LGT")) {
			rslt = "L";
		}
		else {
			rslt = "U";
		} 
		return rslt;
	}

	public static String lengthLimit(String inputStr, int limit, String fixStr) {
		if (inputStr == null)
			return "";
		if (limit <= 0)
			return inputStr;
		byte[] strbyte = null;
		strbyte = inputStr.getBytes();

		if (strbyte.length <= limit) {
			return inputStr;
		}
		char[] charArray = inputStr.toCharArray();
		int checkLimit = limit;
		
		// 대상 문자열 마지막 자리가 2바이트의 중간일 경우 제거함
		for (int i = 0; i < charArray.length; i++) {
			if (charArray[i] < 256) {
				checkLimit -= 1;
			} else {
				checkLimit -= 2;
			}
			if (checkLimit <= 0) {
				break;
			}
		}
		
		byte[] newByte = new byte[limit + checkLimit];
		for (int i = 0; i < newByte.length; i++) {
			newByte[i] = strbyte[i];
		}
		if (fixStr == null) {
			return new String(newByte);
		} else {
			return new String(newByte) + fixStr;
		}
	}




	public static String changeRsltCodeData(String data) {
		String rslt="";
		switch (data) {
			case "Success":
			rslt = "0";
			break;
		// 300 error code
			case "Invalid Receiver Phone Number":
			rslt = "";
			break;
			case "Server Busy":
			rslt = "";
			break;
			case "Spam Checked":
			rslt = "m";
			break;
			case "Receiver Ban Phone Number":
			rslt = "p";
			break;
			case "Callback Ban Phone Number":
			rslt = "p";
			break;

		// 400 error code	
			case "0/Failure":
			rslt = "1";
			break;
			case "1/Handset Busy":
			rslt = "A";
			break;
			case "2/Handset Shade":
			rslt = "B";
			break;
			case "3/Handset Off":
			rslt = "C";
			break;
			case "4/Handset Full":
			rslt = "D";
			break;

		// 410 Error Code
			case "0/Invalid":
			rslt = "2";
			break;
			case "1/Handset Invalid":
			rslt = "a";
			break;
			case "2/Handset Error":
			rslt = "b";
			break;
			case "3/Handset Refuse":
			rslt = "c";
			break;
			case "4/Unknown Error":
			rslt = "d";
			break;
			case "5/SMC Error":
			rslt = "e";
			break;
			case "6/SMTS Format Error":
			rslt = "f";
			break;
			case "7/Handset No Service":
			rslt = "g";
			break;
			case "8/Handset Denied":
			rslt = "h";
			break;
			case "9/SMC User Delete":
			rslt = "i";
			break;
			case "10/SMC Msg Que Full(019)":
			rslt = "j";
			break;
			case "11/Spam":
			rslt = "k";
			break;

			default:
			rslt = "?";
			break;
		}
		return rslt;
	}

}
