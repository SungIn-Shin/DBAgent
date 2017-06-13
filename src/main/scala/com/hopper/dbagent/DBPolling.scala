package com.hopper.dbagent

import java.lang.Exception

import com.hopper.dbagent.util.{Logging, StateThread}

import com.hopper.dbagent.common.IAgentDBCore;
import com.hopper.dbagent.common.IAgentTS;
import com.hopper.dbagent.vo.MessageJobVO;
import com.hopper.dbagent.vo.MessageTodayStatsVO;
import com.hopper.dbagent.vo.MessageStatsVO;
import com.hopper.dbagent.vo.MessageTempVO;
import com.hopper.dbagent.vo.MessageVO;

import java.util.Date;
import play.api.libs.json._
import scala.collection.JavaConverters._

package DBStatus {
  sealed trait Status { def tag: String }
  case object Connecting    extends Status { val tag = "connecting" }
  case object Connected     extends Status { val tag = "running" }
  case object Disconnected  extends Status { val tag = "disconnected" }
}


class DBPolling(val app: IAgentTS) extends StateThread with Logging {

  @volatile var status: DBStatus.Status = DBStatus.Disconnected

  private var db: IAgentDBCore  = null

  private var mybatisConfFile: String = _
  private var standAlone: String = _
  private var xmlFile: String = _
  private var xmlNS: String = _

  private var url: String = _
  private var id: String = _
  private var passwd: String = _

  def loadProperties() {
    val config = hpr.util.Config.cfg()

    this.mybatisConfFile = config.get("app.db.mybatisConfFile")
//    this.xmlFile         = config.get("app.db.xmlFile")
    this.standAlone      = config.get("app.db.standAlone")
    this.xmlNS           = config.get("app.db.xmlNs")
    this.url             = config.get("app.db.url")
    this.id              = config.get("app.db.name")
    this.passwd          = config.get("app.db.pwd")
    
    if (this.standAlone == "Y") {
    	this.xmlNS 	= "hsqldb"
      this.url 		= "jdbc:hsqldb:file:dat/inprocessdb/clientDB;shutdown=true;sql.syntax_ora=true;hsqldb.default_table_type=cached"
      this.id  		= "SA"
      this.passwd = ""
    }
    this.xmlFile	= "cfg/mybatis-" + this.xmlNS + "-dbagent.xml"
    
    log.info(s"Config - DB: ${this.xmlFile}, ${this.xmlNS}, ${this.url}, ${this.id}, ${this.passwd}")
  }


  def init() {
    try {
      status = DBStatus.Connecting
      log.info(s"Load dbHandler")
      db = new AgentDBImpl()
      
      log.info(s"Connect to db: $xmlFile, $xmlNS, $url, $id")
      db.connectDB(mybatisConfFile, xmlFile, xmlNS, url, id, passwd)
      status = DBStatus.Connected
    } 
    catch {
      case ex: Exception => {
        log.warn(s"${StackTrace.toString(ex)}")
        status = DBStatus.Disconnected
        db = null
      }
    }
  }

  def connectTest: (Boolean, String) = {
    try { 
      log.info(s"Test Connect to db: $xmlFile, $xmlNS, $url, $id")

      MybatisHandler.makeMybatisHandler("TEST", url, id, passwd, mybatisConfFile, xmlFile )
      (true, "Connected")
    } 
    catch {
      case ex: Exception => {
        log.warn(s"Connect TEST - $ex")
        (false, "Connection Fail")
      }
    }
    finally {
      MybatisHandler.removeMybatisHandler("TEST")
    }
  }

  def updateSentFail(msgKey: String, data: String): Boolean = {
    if(status == DBStatus.Connected) {
      db.updateSentFail(msgKey, data)
    }
    else false
  }

  def updateReport(msgKey: String, date: String, code: String, data: String, net: String): Boolean = {
    if(status == DBStatus.Connected) {
      db.updateReport(msgKey, date, code, data, net)
    }
    else false
  }

  def insertMsg(phone: String, callback: String, text: String, msgType: String): Boolean = {
   if(status == DBStatus.Connected) {
    db.insertMsg(phone, callback, text, msgType, "N")
  }
  else false   
}

def excelDataInsert(excelFile: String, jobKey: String, jobName: String, msgType: String, callback: String, resvYN: String, sendDate: Date): Int = {
  val insertCnt = db.excelDataInsert(excelFile, jobKey, jobName, msgType, callback, resvYN, sendDate)
  return insertCnt
}


def excelDataTest(excelFile: String, jobName: String, msgType: String
                 ,callback: String, resvYN: String, sendDate: Date): hpr.util.Pair[List[Object],List[Object]] = {
  val rsltTestData = db.excelDataTest(excelFile, jobName, msgType, callback, resvYN, sendDate).asInstanceOf[hpr.util.Pair[List[Object],List[Object]]]
  return rsltTestData
}



def selectJobList(pageNo: Int, 
  offset: Int,
  limit: Int, 
  beginSendDate: String,
  endSendDate: String,
  callback: String,
  jobKey: String,
  msgType: String,
  resvYN: String
  ): List[MessageJobVO] = {
  val jobList = db.selectJobList(pageNo, offset, limit, beginSendDate, endSendDate, callback, jobKey, msgType, resvYN).asScala.toList
  return jobList
}

def selectJobListCnt(beginSendDate: String,
  endSendDate: String,
  callback: String,
  jobKey: String,
  msgType: String,
  resvYN: String
  ): Int = {
  val count = db.selectJobListCnt(beginSendDate, endSendDate, callback, jobKey, msgType, resvYN)
  return count
}


def selectMsgJobDataListCnt(jobKey:  String, phone:   String, rsltCode:String, status:  String): Int = {
  val count = db.selectMsgJobDataListCnt(jobKey, phone, rsltCode, status)
  return count 
}

def selectMsgJobDataList(pageNo:  Int, 
 offset: Int,
 limit:   Int, 
 jobKey:  String, 
 phone:   String, 
 rsltCode:String, 
 status:  String
 ): List[MessageVO] = {
  val detailList = db.selectMsgJobDataList(pageNo, offset, limit, jobKey, phone, rsltCode, status).asScala.toList
  return detailList
}

def selectMsgDataListCnt( beginSendDate: String,
  endSendDate: String,
  phone:   String, 
  callback:String, 
  msgKey:  String, 
  msgType:  String,
  resvYN:  String, 
  status: String, 
  rsltCode: String
  ): Int = {
  val count = db.selectMsgDataListCnt(beginSendDate, endSendDate, phone, callback, msgKey, msgType, resvYN, status, rsltCode)
  return count
}

def selectMsgDataList(pageNo:  Int, 
  offset: Int,
  limit:   Int, 
  beginSendDate: String,
  endSendDate: String,
  phone:   String, 
  callback:String, 
  msgKey:  String, 
  msgType:  String,
  resvYN:  String, 
  status: String, 
  rsltCode: String
  ): List[MessageVO] = {
  val msgList = db.selectMsgDataList(pageNo, offset, limit, beginSendDate, endSendDate, phone, callback, msgKey, msgType, resvYN, status, rsltCode).asScala.toList
  return msgList
}

def selectMsgStatsList(statsType: String, yyyy: String, mm: String, msgType: String, sentRslt: String): List[MessageStatsVO] = {
  val statsList = db.selectMsgStatsList(statsType, yyyy, mm, msgType, sentRslt).asScala.toList
  return statsList
}


def selectMsgDataTodayReady(): List[MessageTodayStatsVO] = {
  val waitCntList = db.selectMsgDataTodayReady().asScala.toList
  return waitCntList
}

def selectMsgDataTodaySuccFail(): List[MessageTodayStatsVO] = {
  val succCntList = db.selectMsgDataTodaySuccFail().asScala.toList
  return succCntList
}



def run() {

  var dbIdle = 0
  var queryIdle = 0

  while(keepGoing) {
    if (db == null && dbIdle % 5 == 0) {
      init()
      dbIdle = 0
    }
    if (db == null) {
      dbIdle += 1
      try { Thread.sleep(1000) } catch { case ex: InterruptedException => }
    } 
    else {
      if (queryIdle % 300 == 0) {
        log.info(s"polling db")
        queryIdle = 0
      }
      queryIdle += 1
      val sentCnt = db.selectAndUpdate(app)
      if (sentCnt == 0) {
        try { Thread.sleep(1000) } catch { case ex: InterruptedException => }
      }
      val moveCnt = db.selectAndInsertDelete()
      if (moveCnt == 0) {
        log.debug(s"move data is empty")
      }
      }       
    }

    log.info(s"Finish DB Polling")
    status = DBStatus.Disconnected
    db = null
  }



  /*

  import java.io.File
  import java.net.URL
  import java.net.URLClassLoader
  def init {
    try {
      MybatisHandler.makeMybatisHandler (
      "test",
      "jdbc:oracle:thin:@210.206.96.35:1521:igov",
      "b2b_gw_dev",
      "b2b_gw_dev",
      "cfg/mybatis-config.xml",
      "cfg/mybatis-oracle.xml"
      );


    }
    catch {
      case e: java.io.IOException => println(s"$e")
    }
  }

  def insert {
    val msg = new MessageVO

    for (i <- 1 to 1000) {
      msg.msgKey = i + ""
      msg.phone = (1011110000 + i) + ""
      msg.callback = "1004"
      msg.reqDate = new java.util.Date()
      msg.msgType = "1"
      msg.text = "test data"
      msg.status = 0
      try {
        MybatisHandler.getInstance("test").exec( "ORACLE", "insert_msg", msg);
      }
      catch {
        case e: java.io.IOException => println(s"$e")
      }
    }
  }


  def main(args : Array[String]) : Unit = {

    processInput()

    try {
      var classLoader = new java.net.URLClassLoader(
      Array(new File("npro1.jar").toURI.toURL),this.getClass.getClassLoader)

      val cl = classLoader.loadClass("com.hopper.db.ClientDBHandlerImplNpro1");
      val handler = cl.newInstance().asInstanceOf[ClientDBHandler]
      handler.read()
    }
    catch {
      case unexpected: Throwable => println(s"$unexpected")
    }
  }
  */

}
