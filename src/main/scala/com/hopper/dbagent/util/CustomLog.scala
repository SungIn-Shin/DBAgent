// package com.hopper.dbagent;

// import ch.qos.logback.classic.Level; 
// import ch.qos.logback.classic.LoggerContext; 
// import ch.qos.logback.classic.joran.JoranConfigurator; 
// import ch.qos.logback.classic.spi.ILoggingEvent; 
// import ch.qos.logback.core.Appender; 
// import ch.qos.logback.core.AppenderBase; 
// import org.slf4j.Logger; 
// import org.slf4j.LoggerFactory

// public class CustomLog {
// 	static public void addAppender()
// 	{
// 	  LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
// 	  JoranConfigurator jc = new JoranConfigurator();
// 	  jc.setContext(lc);


// 	  Appender appender = new AppenderBase<ILoggingEvent>()
// 	  {
// 	    @Override
// 	    protected void append(ILoggingEvent event)
// 	    {
// 	    	System.out.println(event);
// 	    }
// 	  };
// 	  ch.qos.logback.classic.Logger rootLogger = lc.getLogger(
// 	    Logger.ROOT_LOGGER_NAME);
// 	  rootLogger.addAppender(appender);
// 	  appender.start();
// 	}
	 
// }
