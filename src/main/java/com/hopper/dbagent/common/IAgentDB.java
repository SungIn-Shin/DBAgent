package com.hopper.dbagent.common;


public interface IAgentDB   {
  public boolean updateSentFail(String msgKey, String data);
  public boolean updateReport(String msgKey, String date, String code, String data, String net);
}