package com.hopper.dbagent.util

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.Appender; 
import ch.qos.logback.core.AppenderBase; 

import org.slf4j.LoggerFactory
import org.slf4j.Logger.ROOT_LOGGER_NAME

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger

import play.api.libs.json._


trait Logging {
  protected lazy val log = LoggerFactory.getLogger(getClass.getName)

  protected def setLogLevel(level: String) = { 
  	val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
  	val root = logger.asInstanceOf[Logger]
  	level match {
      case "debug"	=> root.setLevel(Level.DEBUG)
      case "info"		=> root.setLevel(Level.INFO)
      case _ 				=> root.setLevel(Level.INFO)
    }
  }
}

object Logging {
  def addAppender(cb: JsValue => Unit): Unit = {

    val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    val jc = new JoranConfigurator();
    jc.setContext(lc);
    lc.reset();

    try {
      jc.doConfigure("cfg/logback.xml");
    } catch {
      case e: JoranException => e.printStackTrace();
    }

    // val encoder = new PatternLayoutEncoder();
    // encoder.setContext(lc)
    // encoder.setPattern("%-5level %d{yyyy-MM-dd HH:mm:ss} [%thread] %logger{36} - %msg%n")
    // encoder.start()


    val appender = new AppenderBase[ILoggingEvent] {
      @Override
      protected def append(event: ILoggingEvent): Unit = {
        // println(s"event.getFormattedMessage(): ${event.getFormattedMessage()}")
        // println(s"event.getLevel(): ${event.getLevel()}")
        // println(s"event.getLoggerName(): ${event.getLoggerName()}")
        // println(s"event.getMessage(): ${event.getMessage()}")
        // println(s"event.getThreadName(): ${event.getThreadName()}")
        // println(s"event.getTimeStamp(): ${event.getTimeStamp()}")
        val value: JsValue = Json.obj("msg" -> event.getMessage(),
                    "level" -> event.getLevel().toString(),
                    "logger" -> event.getLoggerName(),
                    "thread" -> event.getThreadName(),
                    "time" -> event.getTimeStamp())
        cb(value)
        
      }
    }

    val rootLogger: Logger = lc.getLogger(ROOT_LOGGER_NAME)
    rootLogger.addAppender(appender.asInstanceOf[Appender[ILoggingEvent]])
    appender.start()
  }
}
