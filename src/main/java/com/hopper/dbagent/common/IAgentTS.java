package com.hopper.dbagent.common;


public interface IAgentTS
{
  public boolean isWritable();
  
  public boolean sendSms(	String msgKey,
  												String phone,
  												String callback,
  												String contentType,
  												String text
  												);

  public boolean sendMms(	String msgKey, 
  												String phone,
  												String callback,
  												String contentType,
  												String subject,
  												String text,
  												String fileName1,
  												String fileType1,
  												String fileName2,
  												String fileType2,
  												String fileName3,
  												String fileType3
  												);
}