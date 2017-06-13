package com.hopper.dbagent

import java.io.File
import java.net.URL
import java.net.URLClassLoader

import scala.concurrent.Future

import com.hopper.dbagent.umgp._
import com.hopper.dbagent.util.Logging

import com.hopper.dbagent.common.IAgentDB;
import com.hopper.dbagent.common.IAgentTS;

import com.hopper.dbagent.http._
import java.util.Date

import com.hopper.dbagent.vo.MessageTempVO;

import com.hopper.dbagent.common.IAgentDBCore;
import java.util.Date;
import java.text.SimpleDateFormat;
//import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
    

import play.api.libs.json._

object App extends Command with IAgentTS with IAgentDB with Logging {

  private var db: AgentDBImpl = _
  private var configFilePath: String = _
  @volatile var keepGoing: Boolean = true

  sealed trait Menu { def name: String}
  case object TS extends Menu   { val name = "TS" }
  case object DB extends Menu   { val name = "DB" }
  case object WEB extends Menu  { val name = "WEB" }

  val blankRE = """^\s*#?\s*$""".r
  val tsStrNumRE = """^\s*ts\s+(.+)\s+(\d+)\s+(.+)\s+(.+)$""".r
  val dbStrStrStrRE = """^\s*db\s+(.+)\s+(.+)\s+(.+)\s+(.+)\s+(.+)$""".r
  val startStringRE = """^\s*start\s+(all|ts|db|web)$""".r
  val stopStringRE = """^\s*stop\s+(all|ts|db|web)$""".r
  val statusStringRE = """^\s*status\s+(all|ts|db|web)$""".r
  val logLevelStringRE = """^\s*log\s+(debug|info)$""".r

  def prompt() = print(">> ")
  def invalidInput(s: String) =
  println(s"Unrecognized command: $s")
  def invalidTarget(c: String): Unit =
  println(s"Expected 'ts', 'db', or 'all'. Got $c")

  def getLine: PartialFunction[String,JsValue] = synchronized {
    case "status" => {
      Json.obj( "db"-> dbPolling.status.tag,
        "ts"-> umgpModule.sendLineStatus.tag)
    }
  }

  def handleLine: PartialFunction[String,Unit] = {
    case blankRE() =>   // do nothing
    case "h" | "help" => println(help)
    //case "i" | "insert" => insert()
    case tsStrNumRE(ip, port, id, pwd) => setupTS(ip, port.toInt, id, pwd)
    case dbStrStrStrRE(xmlFile, xmlNs, url, id, passwd) => setupDB(xmlFile, xmlNs, url, id, passwd)
    case startStringRE(target) => target match {
      case "ts" => start(TS)
      case "db" => start(DB)
      case "web" => start(WEB)
      case "all" => startAll
      case _ => invalidTarget(target)
    }
    case stopStringRE(target) => target match {
      case "ts" => stop(TS)
      case "db" => stop(DB)
      case "web" => stop(WEB)
      case "all" => stopAll
      case _ => invalidTarget(target)
    }
    case logLevelStringRE(level) => changeLogLevel(level)
    case "q" | "quit" | "exit" => finished()
    case string => invalidInput(string)
  }
  private def finished(): Unit = {
    keepGoing = false
    dbPolling.stop
    umgpModule.stop
    httpServer.stop

    log.info("App finished")
    //exit("Goodbye!", 0)
  }
  private def processInput(): Unit = {

/*
    val handleLine: PartialFunction[String,Unit] = {
      case blankRE() =>   // do nothing
      case "h" | "help" => println(help)
      //case "i" | "insert" => insert()
      case tsStrNumRE(ip, port, id, pwd) => setupTS(ip, port.toInt, id, pwd)
      case dbStrStrStrRE(xmlFile, xmlNs, url, id, passwd) => setupDB(xmlFile, xmlNs, url, id, passwd)
      case startStringRE(target) => target match {
        case "ts" => start(TS)
        case "db" => start(DB)
        case "web" => start(WEB)
        case "all" => startAll
        case _ => invalidTarget(target)
      }
      case stopStringRE(target) => target match {
        case "ts" => stop(TS)
        case "db" => stop(DB)
        case "web" => stop(WEB)
        case "all" => stopAll
        case _ => invalidTarget(target)
      }
      case logLevelStringRE(level) => changeLogLevel(level)
      case "q" | "quit" | "exit" => finished()
      case string => invalidInput(string)
    }
    */
    while (keepGoing) {
      prompt()
      Console.in.readLine() match {
        case null => finished()
        case line => handleLine(line)
      }
    }
  }

  private val help =
  """
  |--------------------------------------------------------------------------
  |Usage: Bridge [-h | --help]
  |Then, enter one of the following commands, one per line:
  |  h | help                   Print this help message.
  |  server ip port             Setup server's IP and PORT.
  |  db xml ns host id passwd   Setup database's HOST, ID and PASSWORD.
  |  start [all|db|ts|web]      Start all, db or ts threads .
  |  stop [all|db|ts|web]       Stop all, db or ts threads .
  |  log [debug|info]           Change log level between debug or info .
  |  ^d | quit                  Quit.
  |--------------------------------------------------------------------------
  |""".stripMargin
/*
  private def exit(message: String, status: Int): Nothing = {
    println(message)
    sender.isRunning = false
    //sys.exit(status)
  }
  */

  private def startAll(): Unit = {
    start(TS)
    start(DB)
    start(WEB)
  }

  private def stopAll(): Unit = {
    stop(TS)
    stop(DB)
    stop(WEB)
  }

  private def start(menu: Menu): Unit = synchronized {
    log.info(s"Start ${menu.name}")
    menu match {
      case TS => umgpModule.start
      case DB => dbPolling.start
      case WEB => if (httpServer != null) httpServer.start
    }
  }

  private def stop(menu: Menu): Unit = synchronized {
    log.info(s"Stop ${menu.name}")
    menu match {
      case TS => umgpModule.stop
      case DB => dbPolling.stop
      case WEB => if (httpServer != null) httpServer.stop
    }
  }


  private def setupTS(ip: String, port: Int, id: String, pwd: String): Unit = {
    stop(TS)
    //log.info(s"Setup TS: ip=$ip port=$port id=$id pwd=$pwd");
    //umgpModule.setup(ip, port, id, pwd)
  }

  private def setupDB(xmlFile: String, xmlNS: String, url: String, id: String, passwd: String): Unit = {
    stop(DB)
    //log.info(s"Setup DB: url=$url id:$id passwd=$passwd");
    //dbPolling.setup(xmlFile, xmlNS, url, id, passwd)
  }

  private def changeLogLevel(level: String): Unit = synchronized {
    log.info(s"Change log level to [$level]");
    setLogLevel(level)
  }

  private val dbPolling = new DBPolling(this)
  private val umgpModule = new UmgpModule(this)
  private val httpServer = new HttpServer(this)

  private val makeKey = new hpr.util.MakeKey(new hpr.util.MakeKey.MakeKeyOpForMsgKey("JOBKEY_"));

  private val logQueue = new java.util.concurrent.LinkedBlockingQueue[JsValue]
  Logging.addAppender((obj: JsValue) => {
    logQueue.add(obj)
    })
  def getLogs: JsValue = synchronized {
    var arr = Json.arr()

    var continue = true
    while(continue) {
      val obj = logQueue.poll()
      if (obj != null) arr = arr :+ obj
      else continue = false
    }
    Json.obj("logs" -> arr)
  }

  def getConfigs: JsValue = synchronized {
    val config = hpr.util.Config.cfg()
  	
    val dbConfig = Json.obj(
      "dbStandAlone" -> config.get("app.db.standAlone"),
      "dbXmlNS" -> config.get("app.db.xmlNs"),
      "dbUrl" -> config.get("app.db.url"),
      "dbUserId" -> config.get("app.db.name"),
      "dbPasswd" -> config.get("app.db.pwd")
      )

    val tsConfig = Json.obj("ip" -> config.get("app.ts.ip"),
      "port" -> config.get("app.ts.port"),
      "id" -> config.get("app.ts.id"),
      "pwd" -> config.get("app.ts.pwd"),
      "useSendLine" -> config.get("app.ts.useSendLine"),
      "useRecvLine" -> config.get("app.ts.useRecvLine")
      )
    Json.obj("db" -> dbConfig, "ts" -> tsConfig)
  }

  def setConfigs(target: String, json: JsValue): JsValue = synchronized {

    val config = hpr.util.Config.cfg()
    if (target == "ts") {
      config.set("app.ts.ip", (json \ "ip").asOpt[String].getOrElse(""))
      config.set("app.ts.port", (json \ "port").asOpt[String].getOrElse(""))
      config.set("app.ts.id", (json \ "id").asOpt[String].getOrElse(""))
      config.set("app.ts.pwd", (json \ "pwd").asOpt[String].getOrElse(""))
      config.set("app.ts.useSendLine", (json \ "useSendLine").asOpt[String].getOrElse(""))
      config.set("app.ts.useRecvLine", (json \ "useRecvLine").asOpt[String].getOrElse(""))
      umgpModule.loadProperties()
    }
    else if (target == "db") {
      config.set("app.db.standAlone", (json \ "dbStandAlone").asOpt[String].getOrElse(""))
      config.set("app.db.xmlNs", (json \ "dbXmlNS").asOpt[String].getOrElse(""))
      config.set("app.db.url", (json \ "dbUrl").asOpt[String].getOrElse(""))
      config.set("app.db.name", (json \ "dbUserId").asOpt[String].getOrElse(""))
      config.set("app.db.pwd", (json \ "dbPasswd").asOpt[String].getOrElse(""))
      dbPolling.loadProperties()
    }
    getConfigs
  }

  def testConnect(target: String): JsValue = synchronized {
    val res =
    if (target == "ts") umgpModule.connectTest
    else dbPolling.connectTest

    log.info(s"$target - $res")

    res match {
      case (result, value) => Json.obj("result" -> result, "value" -> value)
    }
  }

  def getStats: JsValue = synchronized {
    Json.obj( "sentCntSucc" -> umgpModule.sentCntSucc,
      "sentCntFail" -> umgpModule.sentCntFail,
      "recvCntSucc" -> umgpModule.recvCntSucc,
      "recvCntFail" -> umgpModule.recvCntFail)
  }

  def insertMsg(json: JsValue): JsValue = synchronized {
    val result = dbPolling.insertMsg((json \ "phone").asOpt[String].getOrElse(""),
      (json \ "callback").asOpt[String].getOrElse(""),
      (json \ "text").asOpt[String].getOrElse(""),
      (json \ "contentType").asOpt[String].getOrElse("")
      )

    Json.obj("result" -> result)
  }
  
  def excelDataTest(json: JsValue): JsValue = {
    var excelFilePath = (json \ "excelFilePath").asOpt[String].getOrElse("")
    var jobName   = (json \ "jobName").asOpt[String].getOrElse("")
    var msgType   = (json \ "msgType").asOpt[String].getOrElse("")
    var callback  = (json \ "callback").asOpt[String].getOrElse("")
    var sendDate  = (json \ "sendDate").asOpt[String].getOrElse("")
    var resvYN    = if(sendDate.isEmpty()) "N" else "Y"
    
    log.debug(s"[INPUT] [excelFilePath:$excelFilePath/jobName:$jobName/msgType:$msgType/callback:$callback/sendDate:$sendDate/resvYN:$resvYN]")
    var pair = dbPolling.excelDataTest(excelFilePath, jobName, msgType, callback, resvYN, 
      try {
        if (resvYN == "Y") new SimpleDateFormat("yyyy MMddHHmm").parse(sendDate) else new Date()
      }
      catch {
        case e : java.text.ParseException => {
          log.error("Date Parse Exception! Standard format ==> [yyyyMMddHHmm] Change to Now Date. -> " + sendDate)
          new Date()
      }
    }) 
   
    var rows = Json.arr()

    val succList = pair.first().asInstanceOf[java.util.ArrayList[Object]]
    val failList = pair.second().asInstanceOf[java.util.ArrayList[Object]]
    
    var succCnt  = succList.size   
    var failCnt  = failList.size
    var totalCnt = succCnt + failCnt


    // var failListSize = failList.size
    // if (failListSize > 50 ) failListSize = 50
    for(idx <- 0 to failList.size - 1) {
      var tempVO = failList(idx).asInstanceOf[MessageTempVO]
      rows = rows :+ Json.obj(
        "tempNo"      -> tempVO.tempNo, 
        "phone"       -> Json.obj("v"->tempVO.phone, "e"->tempVO.phoneErr), 
        "callback"    -> Json.obj("v"->tempVO.callback, "e"->tempVO.callbackErr), 
        "msgType"     -> Json.obj("v"->tempVO.msgType, "e"->tempVO.msgTypeErr), 
        "text"        -> Json.obj("v"->tempVO.text, "e"->tempVO.textErr),
        "fileName1"   -> tempVO.fileName1, 
        "fileType1"   -> tempVO.fileType1, 
        "fileName2"   -> tempVO.fileName2, 
        "fileType2"   -> tempVO.fileType2, 
        "fileName3"   -> tempVO.fileName3, 
        "fileType3"   -> tempVO.fileType3
      )
    }

    log.debug(s"[OUTPUT] [totalCnt:$totalCnt/succCnt:$succCnt/failCnt:$failCnt/rows:$rows]")
    Json.obj("totalCnt" -> totalCnt, 
             "succCnt" -> succCnt, 
             "failCnt" -> failCnt, 
             "rows" -> rows
    )
  }  


  def excelDataInsert(json: JsValue): JsValue = {
    //excelFile: String, jobName: String, msgType: String, callback: String, sendDate: Date
    var excelFilePath = (json \ "excelFilePath").asOpt[String].getOrElse("")
    var jobName   = (json \ "jobName").asOpt[String].getOrElse("")
    var msgType   = (json \ "msgType").asOpt[String].getOrElse("")
    var callback  = (json \ "callback").asOpt[String].getOrElse("")
    var sendDate  = (json \ "sendDate").asOpt[String].getOrElse("")
    var resvYN    = if(sendDate.isEmpty()) "N" else "Y"
    log.info(s"[INPUT] [excelFilePath:$excelFilePath/jobName:$jobName/msgType:$msgType/callback:$callback/sendDate:$sendDate/resvYN:$resvYN]")

    val jobKey = makeKey.make()
    
    val insertCnt = dbPolling.excelDataInsert( excelFilePath, jobKey, jobName, msgType, callback, resvYN,
      try {
        if (resvYN == "Y") new SimpleDateFormat("yyyyMMddHHmm").parse(sendDate) else new Date()
      }
      catch {
        case e : java.text.ParseException => {
          log.error("Bad Date format! Change to Now -> " + sendDate)
          new Date()
        }
      }
    )
    log.info(s"[OUTPUT] [jobKey:$jobKey]")
    Json.obj(
      "jobKey"     -> jobKey
    )
  }


 // def selectAndInsertDeleteMsgTemp(): JsValue = {
 //  val resultJobKey = dbPolling.selectAndInsertDeleteMsgTemp()
 //  var status = false
 //  if (resultJobKey != null)
 //    status = true


 //  Json.obj(
 //    "status" -> status
 //   ,"jobKey" -> resultJobKey
 //    )
 // }

 def selectJobList( pageNo: Int,
                    offset: Int, 
                    limit: Int,
                    beginSendDate: String,
                    endSendDate: String,
                    callback: String,
                    jobKey: String,
                    msgType: String,
                    resvYN: String
                    ): JsValue = {
  val resultJobList = dbPolling.selectJobList(pageNo, offset, limit, beginSendDate, endSendDate, callback, jobKey, msgType, resvYN)
  val count = dbPolling.selectJobListCnt(beginSendDate, endSendDate, callback, jobKey, msgType, resvYN)
  var rows = Json.arr()

  log.debug(s"[INPUT] [pageNo:$pageNo/limit:$limit/beginSendDate:$beginSendDate/endSendDate:$endSendDate/callback:$callback/jobKey:$jobKey/msgType:$msgType/resvYN:$resvYN]")

  for (list <- resultJobList) {
    rows = rows :+ Json.obj(
                      "jobKey"      -> list.jobKey
                     ,"jobName"     -> list.jobName
                     ,"callback"    -> list.callback
                     ,"regDate"     -> list.regDate
                     ,"sendDate"    -> list.sendDate
                     ,"resvYN"      -> list.resvYN
                     ,"msgType"     -> list.msgType
                     ,"totalCnt"    -> list.totalCnt
                     ,"sendCnt"     -> list.sendCnt
                     ,"rsltSuccCnt" -> list.rsltSuccCnt
                     ,"rsltFailCnt" -> list.rsltFailCnt
                      )
  }
  log.debug(s"[OUTPUT] [count:$count/rows:$rows]")
  Json.obj("count" -> count,
           "rows" -> rows)//Json.arr(Json.obj("aa"->"bb")))
 }

 def selectMsgJobDataList(pageNo:  Int,
                            offset: Int,
                            limit:   Int,
                            jobKey:  String,
                            phone:   String,
                            rsltCode:String,
                            status:  String
                            ): JsValue = {
  log.info(s"[INPUT] [pageNo:$pageNo/limit:$limit/jobKey:$jobKey/phone:$phone/rsltCode:$rsltCode/status:$status]" )

  val count = dbPolling.selectMsgJobDataListCnt(jobKey, phone, rsltCode, status)
  val msgList = dbPolling.selectMsgJobDataList(pageNo, offset, limit, jobKey, phone, rsltCode, status)

  

  var rows = Json.arr()
  for (list <- msgList) {
    if (list.doneDate == null)   {
      // 예약전송 status = 9 (변경예정 협의 안됨) by SungIn
      if(list.status == 1 && ( list.sendDate.getTime() > new Date().getTime() ) ) { list.status = 9 } 

      rows = rows :+ Json.obj(
        "callback"     -> list.callback
        ,"phone"       -> list.phone
        ,"msgType"     -> list.msgType
        ,"status"      -> list.status
        ,"resvYN"      -> list.resvYN
        ,"sendDate"    -> list.sendDate
        ,"doneDate"    -> ""
        ,"phoneDate"   -> ""
        ,"reportDate"  -> ""
        ,"rsltCode"    -> list.rsltCode
        ,"rsltData"    -> list.rsltData
        ,"netCode"     -> list.netCode
        ,"text"        -> list.text
      )
    }
    else {
      rows = rows :+ Json.obj(
        "callback"     -> list.callback
        ,"phone"       -> list.phone
        ,"msgType"     -> list.msgType
        ,"status"      -> list.status
        ,"resvYN"      -> list.resvYN
        ,"sendDate"    -> list.sendDate
        ,"doneDate"    -> list.doneDate
        ,"phoneDate"   -> list.phoneDate
        ,"reportDate"  -> list.reportDate
        ,"rsltCode"    -> list.rsltCode
        ,"rsltData"    -> list.rsltData
        ,"netCode"     -> list.netCode
        ,"text"        -> list.text
      )
    }
    
  }

  log.info(s"[OUTPUT] [count : $count/rows:$rows]")
  Json.obj("count" -> count,
           "rows" -> rows)

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
                            ): JsValue = {
  log.info(s"[INPUT] [pageNo:$pageNo/limit:$limit/beginSendDate:$beginSendDate/endSendDate:$endSendDate/phone:$phone/callback:$callback/msgKey:$msgKey/msgType:$msgType/resvYN:$resvYN/status:$status/rsltCode:$rsltCode]" )

  val count = dbPolling.selectMsgDataListCnt(beginSendDate, endSendDate, phone, callback, msgKey, msgType, resvYN, status, rsltCode)
  val msgList = dbPolling.selectMsgDataList(pageNo, offset, limit, beginSendDate, endSendDate, phone, callback, msgKey, msgType, resvYN, status, rsltCode)
  var rows = Json.arr()

  for (list <- msgList) {
    rows = rows :+ Json.obj(
       "msgKey"      -> list.msgKey
      ,"callback"    -> list.callback
      ,"phone"       -> list.phone
      ,"msgType"     -> list.msgType
      ,"status"      -> list.status
      ,"resvYN"      -> list.resvYN
      ,"sendDate"    -> list.sendDate
      ,"doneDate"    -> list.doneDate
      ,"phoneDate"   -> list.phoneDate
      ,"reportDate"  -> list.reportDate
      ,"rsltCode"    -> list.rsltCode
      ,"netCode"     -> list.netCode
      ,"text"        -> list.text
      )
  }

  log.info(s"[OUTPUT] [count:$count/rows:$rows]")
  Json.obj(
   "count" -> count,
   "rows" -> rows)

 }

 def selectMsgStatsList(statsType: String, yyyy: String, mm: String, msgType: String, sentRslt: String): JsValue = {
  log.debug(s"[INPUT] [statsType:$statsType/yyyy:$yyyy/mm:$mm/msgType:$msgType/sentRslt:$sentRslt]" )
  val statsList = dbPolling.selectMsgStatsList(statsType, yyyy, mm, msgType, sentRslt)
  var rows = Json.arr()
  for (list <- statsList) {
    // 성공 데이터만 
    if (sentRslt == "true") {
      rows = rows :+ Json.obj(
       "yyyy"         -> list.yyyy
      ,"mm"           -> list.mm
      ,"dd"           -> list.dd
      ,"msgType"      -> list.msgType
      ,"succCnt"      -> list.succCnt
      ,"totalCnt"     -> list.totalCnt
      )
    }
    // 실패 건수만
    else if (sentRslt == "false") {
      rows = rows :+ Json.obj(
       "yyyy"         -> list.yyyy
      ,"mm"           -> list.mm
      ,"dd"           -> list.dd
      ,"msgType"      -> list.msgType
      ,"failCnt"      -> (list.toutFailCnt + list.invaFailCnt + list.sentFailCnt)
      ,"toutFailCnt"  -> list.toutFailCnt
      ,"invaFailCnt"  -> list.invaFailCnt
      ,"sentFailCnt"  -> list.sentFailCnt
      ,"totalCnt"     -> list.totalCnt
      )
    }
    // 성공 + 실패 모두
    else {
      rows = rows :+ Json.obj(
       "yyyy"         -> list.yyyy
      ,"mm"           -> list.mm
      ,"dd"           -> list.dd
      ,"msgType"      -> list.msgType
      ,"succCnt"      -> list.succCnt
      ,"failCnt"      -> (list.toutFailCnt + list.invaFailCnt + list.sentFailCnt)
      ,"toutFailCnt"  -> list.toutFailCnt
      ,"invaFailCnt"  -> list.invaFailCnt
      ,"sentFailCnt"  -> list.sentFailCnt
      ,"totalCnt"     -> list.totalCnt
      )
    }
    
  }
  log.debug(s"[OUTPUT] [rows:$rows]")
  Json.obj(
   "rows" -> rows)
 }

 def selectMsgTodayReadyAndSuccAndFail(): JsValue = {
  log.info(s"[INPUT] []" )
  val readyList = dbPolling.selectMsgDataTodayReady()
  val succAndFailList  = dbPolling.selectMsgDataTodaySuccFail()

  var ready = Json.obj("sms"->0, "lms"->0, "mms"->0)
  var succ  = Json.obj("sms"->0, "lms"->0, "mms"->0)
  var fail  = Json.obj("sms"->0, "lms"->0, "mms"->0)

   for(rList <- readyList) {
    ready = ready.as[JsObject] ++ Json.obj(
      rList.msgType.toLowerCase -> rList.readyCnt
    )
  }

  for(succList <- succAndFailList) {
    succ = succ.as[JsObject] ++ Json.obj(
      succList.msgType.toLowerCase -> succList.succCnt
    )
  }

  for(failList <- succAndFailList) {
    fail = fail.as[JsObject] ++ Json.obj(
      failList.msgType.toLowerCase -> failList.failCnt
    )
  }
  log.info(s"[OUTPUT] [ready:$ready succ:$succ fail:$fail]")

  Json.obj(
      "ready" -> ready, 
      "succ"  -> succ, 
      "fail"  -> fail
  )
 }




  def isWritable: Boolean = umgpModule.isWritable()

  def sendSms(msgKey: String, phone: String, callback: String, contentType: String, text: String): Boolean =
  umgpModule.sendSms(msgKey, phone, callback, contentType, text)

  def sendMms(msgKey: String, phone: String, callback: String, contentType: String, subject: String, text: String, fileName1: String, fileType1: String, fileName2: String, fileType2: String, fileName3: String, fileType3: String): Boolean =
  umgpModule.sendMms(msgKey, phone, callback, contentType, subject, text, fileName1, fileType1, fileName2, fileType2, fileName3, fileType3)

  def updateSentFail(msgKey: String, data: String): Boolean =
  dbPolling.updateSentFail(msgKey, data)

  def updateReport(msgKey: String, date: String, code: String, data: String, net: String): Boolean =
  dbPolling.updateReport(msgKey, date, code, data, net)

  def main(args : Array[String]) : Unit = {
    //setLogLevel("debug")
    //setLogLevel("info")
    
    if (args.length != 0) hpr.util.Config.cfg().loadFromFile(args(0))
    else hpr.util.Config.cfg().loadFromFile("cfg/app.conf")

    makeKey.createOpen("cfg", "make_key.dat")


    dbPolling.loadProperties()

    umgpModule.loadProperties()

    httpServer.loadProperties()

    umgpModule.connectTest

    start(TS)
    start(DB)
    start(WEB)
    // Test할때만 사용
    processInput()
    while(true) { Thread.sleep(1000) }
  }
}
