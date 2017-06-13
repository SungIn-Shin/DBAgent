package com.hopper.dbagent.umgp


import java.net.InetSocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelFuture, Channel, ChannelFutureListener}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.string.{StringEncoder, StringDecoder}
import io.netty.handler.codec.{DelimiterBasedFrameDecoder, Delimiters}
import io.netty.handler.logging.{LoggingHandler, LogLevel}

import scala.collection.mutable.Map
import scala.util.{Success, Failure}
import scala.concurrent.{Future,Promise}
import scala.concurrent.ExecutionContext.Implicits.global

import com.hopper.dbagent.util.Logging

class UmgpException(val msg: String) extends Exception(msg) {

}

class UmgpClient(val host: String, val port: Int, val id: String, val pwd: String, val reportCallback: (Report) => Boolean = null) extends Logging {

  private val logPrefix = if (reportCallback == null) "[SEND]" else "[REPORT]"

  class UmgpClientHandler(val umgpSender: UmgpClient) extends SimpleChannelInboundHandler[Umgp] with Logging {

    private var isAuthed = false;

    override def channelInactive(ctx: ChannelHandlerContext): Unit = {
      ctx.close()
      super.channelInactive(ctx)
    }

    override def channelRead0(ctx: ChannelHandlerContext, msg: Umgp): Unit = {
      if (!isAuthed) {
        umgpSender.pop("Connect") match {
          case Some(p) =>
            msg match {
              case Ack(_, "100", _) => p.success(msg); isAuthed = true
              case _ => p.failure(new UmgpException(s"Failed: $msg"))
            }
          case _ => log.error(s"$logPrefix ------------------ BUG!! -------------------- $msg")
        }
      }
      else {
        msg match {
          case r: Report =>
            if (null != reportCallback) {
                val result = reportCallback(r)
                if (result) {
                  val ack = Ack(r.key, "100", "")

                  ctx.writeAndFlush(ack)
                  log.info(s"$logPrefix SEND REPORT ACK: $ack")
                }
                else {
                  val ack = Ack(r.key, "300", "")
                  ctx.writeAndFlush(ack)
                  log.warn(s"$logPrefix SEND REPORT FAIL ACK: $ack")
                }
            }
         case Pong(key) =>
            umgpSender.pop(key) match {
              case Some(p) => p.success(msg)
              case _ => log.warn(s"$logPrefix Recved unmatched msg: $msg")
            }
          case Ack(key, "100", _) =>
            umgpSender.pop(key) match {
              case Some(p) => p.success(msg)
              case _ => log.warn(s"$logPrefix Recved unmatched msg: $msg")
            }
          case Ack(key, _, _) =>
            umgpSender.pop(key) match {
              case Some(p) => p.failure(new UmgpException(s"{$msg.data}"))
              case _ => log.warn(s"$logPrefix Recved unmatched msg: $msg")
            }

          case _ => log.warn(s"$logPrefix Recved unknown msg: $msg")
        }
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      // Close the connection when an exception is raised.
      log.warn(s"$logPrefix Unexpected exception from read pipe.", cause)
      ctx.close()

      // connect시
      if (umgpSender.connectPromise != null && !isAuthed) {
        log.debug(s"$logPrefix Call failure for connectPromise")
        umgpSender.connectPromise.failure(cause)
      }
    }
  }


  class UmgpClientInitializer(val handler: UmgpClient) extends ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel): Unit = {
      // Create a default pipeline implementation.
      val p = ch.pipeline()

      p.addLast("log", new LoggingHandler(LogLevel.DEBUG))
      p.addLast("framer", new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter():_*))
      p.addLast("encoder", new StringEncoder())
      p.addLast("decoder", new StringDecoder())
      p.addLast("packet-decoder", new UmgpDecoder())
      p.addLast("packet-encoder", new UmgpEncoder())
      p.addLast("handler", new UmgpClientHandler(handler))

    }
  }

  private var group: NioEventLoopGroup = _

  private var connectPromise: Promise[ChannelFuture] = _
  private var channel: Channel = _


  private val merge: Map[String, (Promise[Umgp], Long)] = Map()

  private def push(key: String, promise: Promise[Umgp]): Unit = {
    synchronized {
      merge += (key -> (promise, System.currentTimeMillis()))
    }
  }

  private def pop(key: String): Option[Promise[Umgp]] = {
    synchronized {
      val value = merge.get(key)
      merge -= key
      value match {     
        case v: Some[(Promise[Umgp], Long)] => {
          log.debug(s"$logPrefix Sent elapsed time: ${System.currentTimeMillis() - v.get._2}ms")
          Some(v.get._1)
        }
        case _ => None
      }
    }
  }

  def checkMergeTimeout(ackTimeoutMilisec: Int): Unit = {
    synchronized {
      val old = merge.filter {
        case (key, value) => { System.currentTimeMillis() - value._2 > ackTimeoutMilisec }
      }
      old foreach {
        case (key, value) => {
          value._1.failure(new UmgpException(s"$logPrefix Ack Timeout: too long to wait for ack ${System.currentTimeMillis() - value._2}ms"))
          merge.remove(key)
        }
      }
    }
  }

  def isWritable(ackWaitCnt: Int): Boolean = {
    this.channel != null && this.channel.isWritable() && this.channel.isActive() && merge.size < ackWaitCnt
  }

  def send(msg: Umgp): Future[Umgp] = {

    val promise = Promise[Umgp]()

    this.channel.writeAndFlush(msg).addListener (
      new ChannelFutureListener {
        def operationComplete(f: ChannelFuture) = {
          if(f.isSuccess()) {
            push(msg.key, promise)
            log.debug(s"$logPrefix Sent complete: $msg")
          } 
          else {
            promise.failure(f.cause())
          }
        }   
      }
    )
    promise.future
  }


  def connect(): Future[ChannelFuture]  = {
    disconnect()

    group = new NioEventLoopGroup()
    val b = new Bootstrap()
      .group(group)
      .channel(classOf[NioSocketChannel])
      .remoteAddress(new InetSocketAddress(host, port))
      .handler(new UmgpClientInitializer(this))

    connectPromise = Promise[ChannelFuture]()
    try {
     
      log.info(s"$logPrefix Connect to $host:$port")
      this.channel = b.connect().sync().channel()
      log.debug(s"$logPrefix Connected")

      val isReportLine = if (reportCallback != null) true else false
      val msg = Connect(id, pwd, isReportLine)

      this.send(msg) onComplete {
        case Success(msg) => 
          connectPromise.success(this.channel.closeFuture())
          log.info(s"$logPrefix Success to auth: $id")

        case Failure(t) => {
          t match {
            case _: UmgpException => log.warn(s"$logPrefix Connected Fail: $t")
            case _ => {
              val errors = new java.io.StringWriter();
              t.printStackTrace(new java.io.PrintWriter(errors));

              log.warn(s"$logPrefix Connected Fail: ${errors.toString()}")
            }
          }
          connectPromise.failure(t)
        }
      }
    }
    catch {
      case e: Throwable => connectPromise.failure(e)
    }

    connectPromise.future
  }

  def disconnect(): Unit = {
//    this.channel.close()
    if (group != null) {
      log.info(s"$logPrefix disconnect")
      group.shutdownGracefully()
    }
    group = null;
  }
}
