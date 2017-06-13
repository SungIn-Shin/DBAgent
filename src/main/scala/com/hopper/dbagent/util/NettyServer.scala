package com.hopper.dbagent

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelInitializer, ChannelOption, ChannelPipeline}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel


package util {
  class NettyServer(val port: Int, val handler: (ChannelPipeline) => Unit) {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup()

    def start: Unit = {
      val b = new ServerBootstrap()
      b.option(ChannelOption.SO_BACKLOG, Int.box(1024))
        .group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
//        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer[SocketChannel]() {
            def initChannel(ch: SocketChannel): Unit = {
              val p = ch.pipeline()
              handler(p)
            }
        })
      val ch = b.bind(port).sync().channel()
      //ch.closeFuture().sync()

    }

    def stop: Unit = {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }
}
