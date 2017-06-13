package com.hopper.dbagent.util

import io.netty.channel.{ChannelFuture, Channel}


import scala.util.{Failure, Success, Try}
import scala.concurrent.{Future,Promise}
import scala.concurrent.ExecutionContext.Implicits.global


package AutoReconnectStatus {
  sealed trait Status { def tag: String }
  case object Connecting    extends Status { val tag = "connecting" }
  case object Connected     extends Status { val tag = "connected" }
  case object Doing         extends Status { val tag = "running" }
  case object Disconnected  extends Status { val tag = "disconnected" }
}


import AutoReconnectStatus._


class AutoReconnect extends Logging {
  private var connectFuture: Future[ChannelFuture] = null
  private var closeFuture: ChannelFuture = _

  def doing(body: => Future[ChannelFuture]): Status = synchronized {
    if (null == connectFuture) {
      connectFuture = body

      connectFuture onComplete {
        case Success(cf) =>
          closeFuture = cf
        case Failure(t) =>
          log.warn(s"$t")
          connectFuture = null
      }
      Connecting
    }
    else {
      if (closeFuture == null) Connected
      else if (closeFuture.isDone()) {
        connectFuture = null
        Disconnected
      }
      else Doing

    }
  }
}
