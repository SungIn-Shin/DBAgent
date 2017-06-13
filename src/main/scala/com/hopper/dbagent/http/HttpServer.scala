package com.hopper.dbagent


import com.hopper.dbagent.util.{NettyServer, Logging}

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler, ChannelFutureListener}
import io.netty.channel.{ChannelInitializer, ChannelOption, ChannelPipeline}
import io.netty.channel.DefaultFileRegion
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil;


package http {
  object App {
    def main(args : Array[String]) : Unit = {
      //      final static String index = System.getProperty("user.dir") + "/res/h2/index.html";

      // val server = new HttpServer(new Command{
      //   def handleLine: PartialFunction[String,Unit] = {
      //     case "h" | "help" => println("")
      //   }
      //      def getLine: PartialFunction[String,JsValue] = {

      // })

      // server.setup(8088)
      // server.start
    }
  }

  class HttpServer(val app: Command) extends Logging {
    private var port: Int = 0
    private var server: NettyServer = null
    
    def loadProperties(): Unit = {
      val config = hpr.util.Config.cfg()

      this.port =Integer.parseInt(config.get("app.web.port"))
      val path = config.get("app.web.path")
      log.info(s"Config - WEB: ${this.port}, $path")
    }
    
    def start: Unit = {
      stop

      server = new NettyServer(port, (p: ChannelPipeline) => {
        p.addLast("codec", new HttpServerCodec())
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast("aggregator", new HttpObjectAggregator(1048576))
        p.addLast(new HttpServerHandler())
        p.addLast(new HttpRouteHandler(app))
        })
      server.start
    }

    def stop: Unit = if(server != null) server.stop
  }


  class HttpServerHandler extends SimpleChannelInboundHandler[FullHttpRequest] with Logging {

    override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
      ctx.flush()
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      cause.printStackTrace()
      //log.warn(cause)
      ctx.close()
    }

    def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {

      val queryStringDecoder = new QueryStringDecoder(request.uri)
      var path = queryStringDecoder.path()
     
      if (!HttpServerHandler.sendStaticFile(ctx, request, path)) {
        request.retain
        ctx.fireChannelRead(request)
      }
    }
  }



  object HttpServerHandler extends Logging {

    def sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus): Unit = {
      val buf = Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);

      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    def sendNotFound(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {

      val buf = Unpooled.copiedBuffer("Not Found", CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, buf)
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
      if (HttpUtil.isKeepAlive(request)) {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes())
      val future = ctx.writeAndFlush(response)
      if (HttpUtil.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    }

    def sendStaticFile(ctx: ChannelHandlerContext, request: FullHttpRequest, path: String): Boolean = {
      try {
        val config = hpr.util.Config.cfg()

        val file = new java.io.File(config.get("app.web.path") + path)
        val raf = new java.io.RandomAccessFile(file, "r")

        log.debug(s"send file: $path")
        // Build the response object.
        val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)

        val contentType = util.ContentType.get(file.getPath())
        val encodingType = if (contentType.startsWith("text")) "; charset=UTF-8" else ""

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + encodingType)

        if (HttpUtil.isKeepAlive(request)) {
          // Add 'Content-Length' header only for a keep-alive connection.
          response.headers().set(HttpHeaderNames.CONTENT_LENGTH, raf.length())
          // Add keep alive header as per:
          // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
          response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
        }

        ctx.write(response)
        ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()))
        val future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        if (HttpUtil.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)

        true
      }
      catch {
        case ex: java.io.FileNotFoundException => false
        case _: Throwable => false
      }
    }
  }
}
