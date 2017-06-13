package com.hopper.dbagent.vo;

public class MessageTodayStatsVO {

	public String msgType;
	public int readyCnt;
	public int succCnt;
	public int failCnt;

	public MessageTodayStatsVO(){
		
	}


	@Override
    public String toString() {
        return "MSG_TYPE : " + msgType + ", READY_CNT : " + readyCnt + ", SUCC_CNT : " + succCnt + ", FAIL_CNT : " + failCnt;
    }	
}
