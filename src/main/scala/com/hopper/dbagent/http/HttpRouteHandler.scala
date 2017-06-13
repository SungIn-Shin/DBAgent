package com.hopper.dbagent


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler, ChannelFutureListener}
import io.netty.channel.{ChannelInitializer, ChannelOption, ChannelPipeline}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.multipart._
import io.netty.handler.codec.http.multipart.FileUpload
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType

import io.netty.util.CharsetUtil;
import java.util.Date;
import play.api.libs.json._

import java.util.Map;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.hopper.dbagent.util.Logging

package http {

  object HttpRouteHandler {
    val apiRunRE = """^/api/run/(.+)/(.+)""".r
    val apiLogRE = """^/api/log""".r
    val apiConfigRE = """^/api/config/(.+)""".r
    val apiTestRE = """^/api/test/(.+)""".r
    val apiStatsRE = """^/api/stats""".r
    val apiMsgRE = """^/api/msg""".r
    val apiRE = """^/api/.*""".r
    val reservedRE = """^/(api|auth|components|app|bower_components|assets)/.*""".r

    
    // Job 엑셀 파일 업로드
    val apiExcelsUploadRE       = """^/api/upload/(.+)""".r
    // Job 리스트 조회 [GET /api/excels/]
    val apiExcelsJobListRE      = """^/api/excels""".r
    // Job 메세지 리스트 조회 [GET /api/excels/:jobKey/msgs/]
    val apiExcelsJobMsgListRE   = """^/api/excels/(.+)/msgs""".r
    

    // Msgs (메세지)
    // val apiMsgsMsgListRE   = """^/api/msgs/msg-list""".r
    val apiMsgsMsgListRE = """^/api/msgs""".r
    //Stats (통계)
    val apiStatsMainRE   = """^/api/stats/main""".r
    val apiStatsListRE   = """^/api/stats/(.+)""".r


    // 엑셀 업로드 테스트용 
    val apiExcelsTestUploadRE = """^/api/test/upload/(.+)""".r



    def sendResponse(ctx: ChannelHandlerContext, request: FullHttpRequest, json: JsValue): Unit = {
      val buf = Unpooled.copiedBuffer(json.toString, CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf)
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
      if (HttpUtil.isKeepAlive(request)) {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes())
      val future = ctx.writeAndFlush(response)
      if (HttpUtil.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    }

    val factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE) // Disk if size exceed
  }


  class HttpRouteHandler(val app: Command) extends SimpleChannelInboundHandler[FullHttpRequest] with Logging {

    var decoder: HttpPostRequestDecoder = null

    override def channelUnregistered(ctx: ChannelHandlerContext): Unit = {
    	if (decoder != null) {
    		decoder.cleanFiles()
    	}
    }

    def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {


      val query = new QueryStringDecoder(request.uri)
      val path = query.path()

      log.debug(s"${request.method()} : $path")
      try {

        (request.method(), path) match {
          case (HttpMethod.PUT, HttpRouteHandler.apiRunRE(target, command)) => routeApiRunPut(ctx, request, target, command)
          case (HttpMethod.GET, HttpRouteHandler.apiRunRE(target, command)) => routeApiRunGet(ctx, request, target, command)
          case (HttpMethod.GET, HttpRouteHandler.apiLogRE()) => routeApiLog(ctx, request)
          case (HttpMethod.GET, HttpRouteHandler.apiConfigRE(target)) => routeApiConfig(ctx, request, target)
          case (HttpMethod.PUT, HttpRouteHandler.apiConfigRE(target)) => routeApiConfigUpdate(ctx, request, target)
          case (HttpMethod.PUT, HttpRouteHandler.apiTestRE(target)) => routeApiTest(ctx, request, target)
          case (HttpMethod.GET, HttpRouteHandler.apiStatsRE()) => routeApiStats(ctx, request)
          case (HttpMethod.PUT, HttpRouteHandler.apiMsgRE()) => routeApiMsg(ctx, request)


          

          case (HttpMethod.POST, HttpRouteHandler.apiExcelsTestUploadRE(mode)) => routeExcelsTestUpload(ctx, request, mode)

          case (HttpMethod.POST, HttpRouteHandler.apiExcelsUploadRE(mode)) => routeExcelsUpload(ctx, request, query, mode)
          case (HttpMethod.GET, HttpRouteHandler.apiExcelsJobListRE()) => routeExcelsJobList(ctx, request, query)
          case (HttpMethod.GET, HttpRouteHandler.apiExcelsJobMsgListRE(jobKey)) => routeExcelsJobMsgList(ctx, request, query, jobKey)

          case (HttpMethod.GET, HttpRouteHandler.apiMsgsMsgListRE()) => routeMsgsMsgList(ctx, request, query)
          case (HttpMethod.GET, HttpRouteHandler.apiStatsMainRE()) => routeStatsMainList(ctx, request)
          case (HttpMethod.GET, HttpRouteHandler.apiStatsListRE(statsType)) => routeMsgStatsList(ctx, request, statsType , query)

       //   case HttpMethod.GET, HttpRouteHandler.apiRE() => routeApi(ctx, request)
       //   case HttpRouteHandler.reservedRE(s) => HttpServerHandler.sendNotFound(ctx, request)
          case _ => HttpServerHandler.sendStaticFile(ctx, request, "/index.html")
        }
      }
      catch {
        case e: Exception => {
          log.error(StackTrace.toString(e))

          val json: JsValue =  Json.obj("result"->"error")
          HttpRouteHandler.sendResponse(ctx, request, json)
        }
      }
    }

    def routeApiRunPut(ctx: ChannelHandlerContext, request: FullHttpRequest, target: String, command: String): Unit = {
      log.debug(s"PUT /api/run/$target/$command")
      app.handleLine(s"$command $target")
      val json: JsValue = Json.obj("status"->app.getLine("status"))

      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiRunGet(ctx: ChannelHandlerContext, request: FullHttpRequest, target: String, command: String): Unit = {
      log.debug(s"GET /api/run/$target/$command")
      val json: JsValue =  Json.obj("status"->app.getLine("status"))
      
      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiLog(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
      log.debug(s"GET /api/log/")
      val json: JsValue =  app.getLogs

      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiConfig(ctx: ChannelHandlerContext, request: FullHttpRequest, target: String): Unit = {
      log.debug(s"GET /api/config/")
      val json: JsValue =  app.getConfigs

      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiConfigUpdate(ctx: ChannelHandlerContext, request: FullHttpRequest, target: String): Unit = {

      val jsonBuf: ByteBuf  = request.content()
      val jsonStr = jsonBuf.toString(CharsetUtil.UTF_8)

      log.debug(s"PUT /api/config/$target $jsonStr")
      val json: JsValue =  app.setConfigs(target, Json.parse(jsonStr))

      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiTest(ctx: ChannelHandlerContext, request: FullHttpRequest, target: String): Unit = {
      val json: JsValue =  app.testConnect(target)

      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeApiStats(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
      val json: JsValue =  app.getStats

      HttpRouteHandler.sendResponse(ctx, request, Json.obj("cnt"->json))
    }

    def routeApiMsg(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {

      val jsonBuf: ByteBuf  = request.content()
      val jsonStr = jsonBuf.toString(CharsetUtil.UTF_8)

      log.debug(s"PUT /api/msg $jsonStr")
      val json: JsValue =  app.insertMsg(Json.parse(jsonStr))

      HttpRouteHandler.sendResponse(ctx, request, json)
    }



    def routeApi(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {

      val json: JsValue = Json.obj("name" -> "Watership Down","value" -> 2)

      val buf = Unpooled.copiedBuffer(json.toString, CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf)
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
      if (HttpUtil.isKeepAlive(request)) {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes())
      val future = ctx.writeAndFlush(response)
      if (HttpUtil.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    }


    def routeExcelsJobList(ctx: ChannelHandlerContext, request: FullHttpRequest, query: QueryStringDecoder): Unit = {

      log.debug(s"GET /api/excels/")
      var params = query.parameters(); // Map<String, List<String>>

      var pageNo = if(params.containsKey("pageNo")) { params.get("pageNo").get(0).toInt } else { 1 }
      var offset = if(params.containsKey("offset")) { params.get("offset").get(0).toInt } else { 1 }
      var limit = if(params.containsKey("limit")) { params.get("limit").get(0).toInt } else { 10 }
      
      var beginSendDate = if(params.containsKey("beginSendDate")) { params.get("beginSendDate").get(0) }  else { "" }
      var endSendDate   = if(params.containsKey("endSendDate"))   { params.get("endSendDate").get(0) }    else { "" }
      var callback      = if(params.containsKey("callback"))      { params.get("callback").get(0) }       else { "" }
      var jobKey        = if(params.containsKey("jobKey"))        { params.get("jobKey").get(0) }         else { "" }
      var msgType       = if(params.containsKey("msgType"))       { params.get("msgType").get(0) }        else { "" }
      var resvYN        = if(params.containsKey("resvYN"))        { params.get("resvYN").get(0) }         else { "" }

      val json: JsValue = app.selectJobList(pageNo, offset, limit, beginSendDate, endSendDate, callback, jobKey, msgType, resvYN)
      HttpRouteHandler.sendResponse(ctx, request, json)
    }

    def routeExcelsJobMsgList(ctx: ChannelHandlerContext, request: FullHttpRequest, query: QueryStringDecoder,jobKey: String): Unit = {
      log.debug(s"GET /api/excels/$jobKey/msgs/")

      var params = query.parameters(); 
      var pageNo = if(params.containsKey("pageNo")) { params.get("pageNo").get(0).toInt } else { 1 }
      var offset = if(params.containsKey("offset")) { params.get("offset").get(0).toInt } else { 1 }
      var limit = if(params.containsKey("limit")) { params.get("limit").get(0).toInt } else { 10 }

      var phone      = if(params.containsKey("phone"))        { params.get("phone").get(0) }       else { "" }
      var rsltCode   = if(params.containsKey("rsltCode"))     { params.get("rsltCode").get(0) }    else { "" }
      var status     = if(params.containsKey("status"))       { params.get("status").get(0) }      else { "" }

      val json: JsValue = app.selectMsgJobDataList(pageNo, offset, limit, jobKey, phone, rsltCode, status)
      HttpRouteHandler.sendResponse(ctx, request, json)
    }


    def routeMsgsMsgList(ctx: ChannelHandlerContext, request: FullHttpRequest, query: QueryStringDecoder): Unit = {
      var params = query.parameters(); // Map<String, List<String>>

      var pageNo = if(params.containsKey("pageNo")) { params.get("pageNo").get(0).toInt } else { 1 }
      var offset = if(params.containsKey("offset")) { params.get("offset").get(0).toInt } else { 1 }
      var limit = if(params.containsKey("limit")) { params.get("limit").get(0).toInt } else { 10 }

      var beginSendDate = if(params.containsKey("beginSendDate")) { params.get("beginSendDate").get(0) }  else { "" }
      var endSendDate   = if(params.containsKey("endSendDate"))   { params.get("endSendDate").get(0) }    else { "" }
      var phone         = if(params.containsKey("phone"))         { params.get("phone").get(0) }        else { "" }
      var callback      = if(params.containsKey("callback"))      { params.get("callback").get(0) }     else { "" }
      var msgKey        = if(params.containsKey("msgKey"))        { params.get("msgKey").get(0) }       else { "" }
      var msgType       = if(params.containsKey("msgType"))       { params.get("msgType").get(0) }      else { "" }
      var resvYN        = if(params.containsKey("resvYN"))        { params.get("resvYN").get(0) }       else { "" }
      var status        = if(params.containsKey("status"))        { params.get("status").get(0) }       else { "" }
      var rsltCode      = if(params.containsKey("rsltCode"))      { params.get("rsltCode").get(0) }     else { "" }

      val json: JsValue = app.selectMsgDataList(pageNo, offset, limit, beginSendDate, endSendDate, phone, callback, msgKey, msgType, resvYN, status, rsltCode)
      HttpRouteHandler.sendResponse(ctx, request, json)
    }


    def routeMsgStatsList(ctx: ChannelHandlerContext, request: FullHttpRequest, statsType: String, query: QueryStringDecoder): Unit = {
      var params = query.parameters();

      var yyyy       = if(params.containsKey("yyyy"))     { params.get("yyyy").get(0) }     else { "" }
      var mm         = if(params.containsKey("mm"))       { params.get("mm").get(0) }       else { "" }
      var msgType    = if(params.containsKey("msgType"))  { params.get("msgType").get(0) }  else { "" }
      var sentRslt   = if(params.containsKey("sentRslt")) { params.get("sentRslt").get(0) } else { "" }

      val json: JsValue = app.selectMsgStatsList(statsType, yyyy, mm, msgType, sentRslt)
      HttpRouteHandler.sendResponse(ctx, request, json)
    }


    def routeStatsMainList(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
      val json: JsValue = app.selectMsgTodayReadyAndSuccAndFail()
      HttpRouteHandler.sendResponse(ctx, request, json)
    }


    // Upload 한 Excel Data를 MSG_DATA 테이블로 이동. 즉 전송하는 작업을 수행.
    // def routeExcelsSendMsg(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
    //   val json: JsValue = app.selectAndInsertDeleteMsgTemp()
    //   HttpRouteHandler.sendResponse(ctx, request, json)
    // }

    def routeExcelsUpload(ctx: ChannelHandlerContext, request: FullHttpRequest, query: QueryStringDecoder, mode: String): Unit = {

      if (request.isInstanceOf[HttpRequest]) {
	      try {
	        decoder = new HttpPostRequestDecoder(HttpRouteHandler.factory, request.asInstanceOf[HttpRequest]);

	      } catch {
	        // case e: (HttpPostRequestDecoder.ErrorDataDecoderException =>
	        // case e: HttpPostRequestDecoder.IncompatibleDataDecoderException => {
	        case e: Exception => {
		        log.error(StackTrace.toString(e))
	          ctx.channel().close()
						return
	        }
	      }
	    }

      if (decoder != null) {
      	if (request.isInstanceOf[HttpContent]) {

 					val chunk = request.asInstanceOf[HttpContent];
 					try {
 						decoder.offer(chunk)

 					}
 					catch {
 						case e: Exception => {
		         	log.error(StackTrace.toString(e))
 							ctx.channel().close()
 							return
 						}
 					}
 					if (chunk.isInstanceOf[LastHttpContent]) {

 						val json: JsValue = readHttpDataChunkByChunk(mode)
 						HttpRouteHandler.sendResponse(ctx, request, json)

 						decoder.destroy();
 						decoder = null;
 					}
      	}
      }


    }

    def readHttpDataChunkByChunk(mode: String): JsValue = {

      val uploadPath = System.getProperty("user.dir") + "/dat/excelupload/" + new SimpleDateFormat("yyyMMdd").format(new Date())
      val uploadDir = new java.io.File(uploadPath) 
      if ( !uploadDir.isDirectory() ) uploadDir.mkdirs()

      var json: JsValue = Json.obj("" -> "")


			try {
				while (decoder.hasNext()) {
					val data: InterfaceHttpData = decoder.next()
					if (data != null) {
						try {
							//writeHttpData(data);
							//log.info(s"data: ${data}" )
							if (data.getHttpDataType() == HttpDataType.Attribute) {
						  	val attribute = data.asInstanceOf[Attribute];

								try {
									val key = attribute.getName()
									val value = attribute.getValue()
									log.debug(s"$key: $value")

                  json = json.as[JsObject] + (key -> JsString(value))

								} 
                catch {
									case e: java.io.IOException => ;
								}
							}
						  if (data.getHttpDataType() == HttpDataType.FileUpload) {

						  	var fileUpload = data.asInstanceOf[FileUpload]

						  	fileUpload.renameTo(new java.io.File(uploadPath + "/" + fileUpload.getFilename())); // enable to move into another
						  	// File dest
						  	//decoder.removeFileUploadFromClean(fileUpload); //remove
						  	// the File of to delete file
                json = json.as[JsObject] + ("excelFilePath" -> JsString(uploadPath + "/" + fileUpload.getFilename()))

						  }
						} 
            finally {
							data.release();
						}
					}
				}
			} catch {
				case e: HttpPostRequestDecoder.EndOfDataDecoderException => ;
			}


//        = Json.obj("excelFilePath" -> )
// val returnJson: JsObject = rJson.as[JsObject] + ("imgId" -> Json.toJson(imgId))


//   var excelFilePath = (json \ "excelFilePath").asOpt[String].getOrElse("")


			// log.info("waiting...")
			// Thread.sleep(3000)
			// log.info("done!")
      
      if (mode == "real") {
        app.excelDataInsert(json)
      }
      else {
        app.excelDataTest(json)
      }
		}


    def routeExcelsTestUpload(ctx: ChannelHandlerContext, request: FullHttpRequest , mode: String): Unit = {
      val contentData = request.content().toString(CharsetUtil.UTF_8)
      if (mode == "real") {
        val json: JsValue = app.excelDataInsert(Json.parse(contentData))
        HttpRouteHandler.sendResponse(ctx, request, json)      
      }
      else {
        val json: JsValue = app.excelDataTest(Json.parse(contentData))
        HttpRouteHandler.sendResponse(ctx, request, json)
      }
    }
  }
}
