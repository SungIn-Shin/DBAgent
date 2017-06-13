package com.hopper.dbagent.umgp

import io.netty.buffer.{Unpooled, ByteBuf}
//import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.codec.MessageToMessageEncoder

import io.netty.channel.{ChannelFuture, Channel}
import scala.collection.mutable.Map


import org.apache.commons.codec.binary.Base64

class BytesToBase64(val base64Len: Int) {
  private val buff: Array[Byte] = new Array(base64Len * 3 / 4)
  private var pos: Int = 0

  def flush (sb: StringBuilder): Unit = {
    val temp: Array[Byte] = new Array(pos)
    System.arraycopy(buff, 0, temp, 0, pos)
    val encoded = Base64.encodeBase64String(temp)
    sb.append("IMAGE:").append(encoded).append("\r\n")
  }
  
  def put (sb: StringBuilder, value: String, len: Int): Unit = {
    put(sb, value.getBytes(), len)
  }  
  
  def put (sb: StringBuilder, value: Array[Byte], len: Int): Unit = {
    var valueLen = value.length
    var maxLen = len
    
    while (maxLen > 0) {
      var remainLen = buff.length - pos
      if (remainLen > maxLen) {
        System.arraycopy(value, value.length-valueLen,
                        buff, pos, Math.min(valueLen, maxLen))
        pos += maxLen
        valueLen = 0
        maxLen = 0
      }
      else {
        System.arraycopy(value, value.length-valueLen,
                        buff, pos, Math.min(valueLen, remainLen))
        val encoded = Base64.encodeBase64String(buff)
        sb.append("IMAGE:").append(encoded).append("\r\n")

        java.util.Arrays.fill(buff, 0.toByte)
        pos = 0
        valueLen -= Math.min(valueLen, remainLen)
        maxLen -= remainLen
      }
    }
  }
} 


object Umgp {
  def decode(command: String, in: Map[String, String]): Umgp = {
    command match {
      case "REPORT" =>
        Report(in.getOrElse("KEY",""), in.getOrElse("CODE",""), in.getOrElse("DATE",""), in.getOrElse("DATA",""), in.getOrElse("NET",""))
      case "ACK" =>
        Ack(in.getOrElse("KEY",""), in.getOrElse("CODE",""), in.getOrElse("DATA",""))
      case "PONG" =>
        Pong(in.getOrElse("KEY",""))
      case unexpected =>
        throw new Error("Case not covered Exception")
    }
  }


  // import org.apache.commons.codec.binary.Base64

  // def put(buff: Array[Byte], pos: Int, value: String, len: Int): Int = {
  //   System.arraycopy(value.getBytes(), 0, buff, pos, value.getBytes().length);
  //   pos + len
  // }

  def main(args : Array[String]) : Unit = {


//    Lms("key", "010", "1004", "test").encode
 //   Mms("key", "010", "1004", "IMT", "subject", "body", "src/main/scala/com/hopper/dbagent/util/Logging.scala", "IMG").encode

  }  
}

sealed abstract class Umgp {
  def key: String
  def encode: String
}


case class Connect(val id: String, val passwd: String, val reportLine: Boolean) extends Umgp {
  override val key = "Connect"
  override def encode: String = {
    val report = if (reportLine) "Y" else "N"
    s"BEGIN CONNECT\r\nID:${id}\r\nPASSWORD:${passwd}\r\nREPORTLINE:${report}\r\nVERSION:SMGP/2.0.1\r\nEND\r\n"
  }
}

case class Report(val key:String, val code:String, val date:String, val data:String, val net:String) extends Umgp {
  override def encode: String = {
      s"${key}"
  }
}

case class Ping(val key:String) extends Umgp {
  override def encode: String = {
    s"BEGIN PING\r\nKEY:${key}\r\nEND\r\n"
  }
}

case class Pong(val key:String) extends Umgp {
  override def encode: String = {
    s"BEGIN PONG\r\nKEY:${key}\r\nEND\r\n"
  }
}



case class Sms(val key:String, val phone:String, val callback:String, val text:String) extends Umgp {
  override def encode: String = {
    s"BEGIN SEND\r\nRECEIVERNUM:${phone}\r\nCALLBACK:${callback}\r\nDATA:${text.replaceAll("\\r\\n", "\r\nDATA:")}\r\nKEY:${key}\r\nEND\r\n"
/*
    val sb = new StringBuilder

    sb.append(s"BEGIN SEND\r\nRECEIVERNUM:${phone}\r\nCALLBACK:${callback}\r\n")
    text.split("\\\\r\\\\n").foreach{ str => sb.append(s"DATA:$str\r\n") }
    sb.append(s"KEY:${key}\r\nEND\r\n")

    sb.toString
*/
  }
}
case class Mms(val key:String, val phone:String, val callback:String, val contentType: String, val subject: String, val text: String, val file1: String = "", val fileType1: String = "", file2: String = "", fileType2: String = "", file3: String = "", fileType3: String = "") extends Umgp {
  override def encode: String = {
     
     //   catch case e: java.io.IOException => log.
     //   java.lang.Exception

    if (key == null || key.length == 0) throw new java.lang.Exception("key is not null") 
    if (phone == null || phone.length == 0) throw new java.lang.Exception("phone is not null") 
    if (callback == null || callback.length == 0) throw new java.lang.Exception("callback is not null") 
    if (contentType == null || contentType.length == 0) throw new java.lang.Exception("contentType is not null") 
  
    val sb = new StringBuilder
    
    sb.append(s"BEGIN MMS\r\nSUBJECT:${subject}\r\nRECEIVERNUM:${phone}\r\nCONTENTTYPE:${contentType}\r\nCALLBACK:${callback}\r\n")


    import java.nio.file.{Files, Paths}
    val fileArray1 = if (file1 != null && file1.length > 0) Some(Files.readAllBytes(Paths.get(file1))) else None
    val fileArray2 = if (file2 != null && file2.length > 0) Some(Files.readAllBytes(Paths.get(file2))) else None
    val fileArray3 = if (file3 != null && file3.length > 0) Some(Files.readAllBytes(Paths.get(file3))) else None

    val maxLen = 221 +
      (if (text != null && !text.isEmpty()) 216 + text.getBytes().length else 0) +
      (if (fileArray1.isDefined) 216 + fileArray1.get.length else 0) +
      (if (fileArray2.isDefined) 216 + fileArray2.get.length else 0) +
      (if (fileArray3.isDefined) 216 + fileArray3.get.length else 0) 

    val fileCnt = 0 +
      (if (text != null && text.length > 0) 1 else 0) +
      (if (fileArray1.isDefined) 1 else 0) +
      (if (fileArray2.isDefined) 1 else 0) +
      (if (fileArray3.isDefined) 1 else 0)

    val base64 = new BytesToBase64(128)
    
    base64.put(sb, "__SKN1.0__", 10)
    base64.put(sb, maxLen.toString, 10)
    base64.put(sb, fileCnt.toString, 1)
    base64.put(sb, subject, 100)
    base64.put(sb, "", 100)

    if (text != null && text.length > 0) {
      base64.put(sb, "TXT", 3)
      base64.put(sb, "aaa.txt", 100)
      base64.put(sb, "ALL", 3)
      base64.put(sb, "", 100)
      base64.put(sb, text.getBytes().length.toString, 10)
      base64.put(sb, text, text.getBytes().length)      
    }
    for { file <- fileArray1 } {
      base64.put(sb, fileType1, 3)
      base64.put(sb, Paths.get(file1).getFileName().toString(), 100)
      base64.put(sb, "ALL", 3)
      base64.put(sb, "", 100)
      base64.put(sb, file.length.toString, 10)
      base64.put(sb, file, file.length)      
    }
    for { file <- fileArray2 } {
      base64.put(sb, fileType2, 3)
      base64.put(sb, Paths.get(file2).getFileName().toString(), 100)
      base64.put(sb, "ALL", 3)
      base64.put(sb, "", 100)
      base64.put(sb, file.length.toString, 10)
      base64.put(sb, file, file.length)      
    }
    for { file <- fileArray3 } {
      base64.put(sb, fileType3, 3)
      base64.put(sb, Paths.get(file3).getFileName().toString(), 100)
      base64.put(sb, "ALL", 3)
      base64.put(sb, "", 100)
      base64.put(sb, file.length.toString, 10)
      base64.put(sb, file, file.length)      
    }
    base64.flush(sb)

    sb.append(s"KEY:${key}\r\nEND\r\n")
 //   println(sb.toString)
    sb.toString
  }
}
case class Ack(val key:String, val code:String, val data:String) extends Umgp {
  override def encode: String = {
    s"BEGIN ACK\r\nKEY:${key}\r\nCODE:${code}\r\nDATA:${data}\r\nEND\r\n"
  }
}




class UmgpDecoder extends MessageToMessageDecoder[String] {

  private val READ_COMMAND = 0
  private val READ_HEADER = 1

  private var state = READ_COMMAND
  private var command: String = _
  private var msg = Map[String, String]()

  import java.util.List
  override def decode(ctx: ChannelHandlerContext, in: String, out: List[AnyRef]): Unit = {

    if (state == READ_COMMAND){
      in.split(" ").map(_.trim) match {
        case Array("BEGIN", cmd) =>
  //        println(s"Recevied CMD: $cmd")

          this.command = cmd
          this.msg.clear()
          this.state = READ_HEADER
        case unexpected =>
            println(s"ERR Cmd $unexpected")
      }
    }
    else if(state == READ_HEADER){
      in.split(":").map(_.trim) match {
        case Array(head, tail) =>
          msg = msg + (head -> tail)
      //    println(s"Received key:$head value:$tail")
        case Array("END") =>
          out.add(Umgp.decode(command, msg))
          state = READ_COMMAND
        case unexpected =>
          println(s"ERR $unexpected")
        }
    }
    else {
      throw new Error("Case not covered Exception")
    }
  }
}


class UmgpEncoder extends MessageToMessageEncoder[Umgp] {
  import java.util.List
  override def encode(ctx: ChannelHandlerContext, in: Umgp, out: List[AnyRef]): Unit = {
    out.add(in.encode)
  }
}
