package com.hopper.dbagent.umgp


import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.Channel
//import java.util.concurrent.{Future => JavaFuture}
import java.net.InetAddress
import io.netty.channel.{ChannelFuture, Channel, ChannelFutureListener}

import scala.util.{Failure, Success, Try}
import scala.concurrent.{Await, Future,Promise}
import scala.concurrent.ExecutionContext.Implicits.global


import com.hopper.dbagent.util.{Logging, AutoReconnect, StateThread}
import com.hopper.dbagent.util.AutoReconnectStatus._
import com.hopper.dbagent.util.AutoReconnectStatus.Status

import com.hopper.dbagent.common.IAgentDB;
import com.hopper.dbagent.common.IAgentTSCore;


class UmgpModule(val app: IAgentDB) extends StateThread with IAgentTSCore with Logging {

  @volatile var sendLineStatus: Status = Disconnected
  @volatile var recvLineStatus: Status = Disconnected

  private var sendLine: UmgpClient = _
  private var recvLine: UmgpClient = _

  private var ip: String = _
  private var port: Int = _
  private var id: String = _
  private var pwd: String = _
  private var useSendLine: Boolean = false
  private var useRecvLine: Boolean = false
  private var ackWaitCnt: Int = _
  private var ackTimeoutMilisec: Int = _


  @volatile var sentCntSucc: Int = 0
  @volatile var sentCntFail: Int = 0
  @volatile var recvCntSucc: Int = 0
  @volatile var recvCntFail: Int = 0

  def loadProperties() {

    val config = hpr.util.Config.cfg()

    this.ip   = config.get("app.ts.ip")
    this.port = Integer.parseInt(config.get("app.ts.port"))
    this.id   = config.get("app.ts.id")
    this.pwd  = config.get("app.ts.pwd")
    this.useSendLine  = if (config.get("app.ts.useSendLine").toUpperCase() == "Y") true else false
    this.useRecvLine  = if (config.get("app.ts.useRecvLine").toUpperCase() == "Y") true else false
    this.ackWaitCnt = Integer.parseInt(config.get("app.ts.ackWaitCnt"))
    this.ackTimeoutMilisec = Integer.parseInt(config.get("app.ts.ackTimeoutMilisec"))

    log.info(s"Config - TS: ${this.ip}, ${this.port}, ${this.id}, ${this.pwd}")
  }

  def isWritable(): Boolean = {
    sendLineStatus == Doing && sendLine != null && sendLine.isWritable(ackWaitCnt)
  }

  def sendSms(msgKey: String, phone: String, callback: String, contentType: String, text: String): Boolean = {
    sendMsg (Sms(msgKey, phone, callback, text))
  }

  def sendMms(msgKey: String, phone: String, callback: String, contentType: String, subject: String, text: String, fileName1: String, fileType1: String, fileName2: String, fileType2: String, fileName3: String, fileType3: String): Boolean = {
    sendMsg(Mms(msgKey, phone, callback, contentType, subject, text, fileName1, fileType1, fileName2, fileType2, fileName3, fileType3))
  }

  
  private def sendMsg(msg: Umgp): Boolean = {

    val enable = isWritable()
    if (!enable) {
      log.info(s"[SEND] Not ready send msg: $msg");
    }
    else {
      log.info(s"[SEND] ready to msg: $msg")
      
      sendLine.send(msg) onComplete {
        case Success(msg) => {
          sentCntSucc += 1
          log.info(s"[SEND] Sent Ack Succ: msgKey=${msg.key}")
        }
        case Failure(t) => {
          val errors = new java.io.StringWriter();
          t.printStackTrace(new java.io.PrintWriter(errors));

          sentCntFail += 1
          log.error(s"[SEND] Sent Ack Fail: $msg ${errors.toString()}")
          app.updateSentFail(msg.key, t.toString)
        }
      }
    }
    enable
  }

  def run() {
    if (ip == "" || port == 0 || id == "" || pwd == "") {
      log.info("Not ready to connect TS because of empty information for server")
      this.keepGoing = false
      return
    }
    sendLine = new UmgpClient(ip, port, id, pwd)
    recvLine = new UmgpClient(ip, port, id, pwd, { 
      r => {
        log.info(s"$r")
        val isWritten = app.updateReport(r.key, r.date, r.code, r.data, r.net)
        if (isWritten)  recvCntSucc += 1
        else            recvCntFail += 1
        isWritten
      }
      })
    val retrySend = new AutoReconnect
    val retryRecv = new AutoReconnect

    var lastSentPingTimeForSend = System.currentTimeMillis()
    var lastSentPingTimeForRecv = System.currentTimeMillis()

    var pingKeyForSend = 0
    var pingKeyForRecv = 0

    while(keepGoing) {

      if (useSendLine) {
        try {
          sendLine.checkMergeTimeout(ackTimeoutMilisec)

          sendLineStatus = retrySend.doing {
            sendLine.connect()
          } 
          sendLineStatus match {
            case Connecting   => log.info(s"[SEND] check status: trying to connect")
            case Connected    => log.info(s"[SEND] check status: trying to auth")
            case Doing        => log.debug("[SEND] check status: running")
            case Disconnected => log.info(s"[SEND] check status: disconnected")
          }
          if (sendLineStatus == Doing && System.currentTimeMillis() - lastSentPingTimeForSend > 6000) {
            lastSentPingTimeForSend = System.currentTimeMillis()
            pingKeyForSend += 1
            sendLine.send(Ping("PING_" + pingKeyForSend))
          }
        }
        catch {
          case t: Exception => log.error(s"[SEND] $t")
          sendLine.disconnect()
        }
      }

      if (useRecvLine) {
        try {
          recvLineStatus = retryRecv.doing {
            recvLine.connect()
          } 
          recvLineStatus match {
            case Connecting   => log.info(s"[RECV] check status: trying to connect")
            case Connected    => log.info(s"[RECV] check status: trying to auth")
            case Doing        => log.debug("[RECV] check status: running")
            case Disconnected => log.info(s"[RECV] check status: disconnected")
          }
          if (recvLineStatus == Doing && System.currentTimeMillis() - lastSentPingTimeForRecv > 6000) {
            lastSentPingTimeForRecv = System.currentTimeMillis()
            pingKeyForRecv += 1
            recvLine.send(Ping("PING_" + pingKeyForRecv))
          }
        }
        catch {
          case t: Exception => log.error(s"[RECV] $t")
          recvLine.disconnect()
        }
      }
      try { Thread.sleep(1000) } catch { case _: InterruptedException => }
    }
    
    log.info("TS has been FINISHED!!!!!")
    sendLine.disconnect()
    sendLine = null
    sendLineStatus = Disconnected

    recvLine.disconnect()
    recvLine = null
    recvLineStatus = Disconnected
  }

  def connectTest: (Boolean, String) = {
    val sendLineTest = new UmgpClient(ip, port, id, pwd)
    val connectFuture: Future[ChannelFuture] = sendLineTest.connect()

    import scala.concurrent.duration._
    
    val result = Await.ready(connectFuture, 5 seconds).value.get
    try {

      result match {
        case Success(ch) => {
          //          ch.awaitUninterruptibly(5, java.util.concurrent.TimeUnit.SECONDS)
          (true, "Connected")
        }
        case Failure(t: UmgpException) => {
          log.warn(s"Connection Test: $t")
          (false, "Auth Failed")
        }
        case Failure(e) => {
          log.warn(s"Connection Test: $e")
          (false, "Connection Failed")
          }      }
        }
        finally {
          sendLineTest.disconnect()
        }

      }
    }
/*

object App extends Logging {

  var server: HttpServer = new HttpServer

  val sender = new UmgpSender
  def main(args: Array[String]) {
    (new Thread(sender)).start()

    var i = 0
    while(i < 10000) {
      if (sender.isWritable()) {
        sender.sendMsg(Sms(i + "", "0102222111" + i, "021004", "TEST" + i))
        i += 1
      }
      else {
        Thread.sleep(100)
        log.info("Wait!!!!")

      }

    //server.bind(9002)
    }
    log.info("FINISHED!!!!")

  }
}

*/
