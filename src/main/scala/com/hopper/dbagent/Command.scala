package com.hopper.dbagent

import play.api.libs.json._
import java.util.Date;
trait Command {
	def getLine: PartialFunction[String,JsValue]
	def handleLine: PartialFunction[String,Unit]
	def getLogs: JsValue
	def getConfigs: JsValue
	def setConfigs(target: String, json: JsValue): JsValue
	def testConnect(target: String): JsValue
	def getStats: JsValue
	def insertMsg(json: JsValue): JsValue
	//def excelDataInsertToMsgTemp(excelFile: String, jobName: String, msgType: String, callback: String, sendDate: Date): JsValue
	def excelDataInsert(json: JsValue): JsValue
	def excelDataTest(json: JsValue): JsValue
	
	def selectJobList(pageNo: Int,
                    offset: Int, 
                    limit: Int, 
                    beginSendDate: String,
                    endSendDate: String,
                    callback: String,
                    jobKey: String,
                    msgType: String,
                    resvYN: String
                    ): JsValue



	def selectMsgJobDataList(pageNo:  Int, 
                            offset: Int, 
                            limit:   Int, 
                            jobKey:  String, 
                            phone:   String, 
                            rsltCode:String, 
                            status:  String
                            ): JsValue


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
                            ): JsValue

	def selectMsgStatsList(statsType: String, yyyy: String, mm: String, msgType: String, sentRslt: String): JsValue

    def selectMsgTodayReadyAndSuccAndFail(): JsValue
	
	}