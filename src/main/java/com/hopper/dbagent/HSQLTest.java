package com.hopper.dbagent.excel;

import com.hopper.dbagent.vo.MessageTempVO;

import com.hopper.dbagent.excel.ExcelHandler;
import com.hopper.dbagent.MybatisHandler;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class HSQLTest {

	private static MybatisHandler db = null;
	private static String DB_NAMESPACE = "hsqldb";

	public static void main(String[] args) {
		try {
			long start = 0 , end = 0;

			start = System.currentTimeMillis();
			HSQLTest h = new HSQLTest();
			h.connectDB("cfg/mybatis-hsqldb-dbagent.xml", 
				"hsqldb",
				//"jdbc:hsqldb:mem:mymemdb",
				"jdbc:hsqldb:file:hsqldb/testdb;ifexists=true;shutdown=true",
				/*"jdbc:hsqldb:file:D:/tt/tt/test",*/
				"SA",
				""
				);
			System.out.println("DB접속 성공");
			// db.exec(DB_NAMESPACE, "create_msg_job_table", null);
			// System.out.println("테이블 생성  성공");
			Map<String, Object> map = new HashMap<>();
			map.put("jobKey", "123431235123");
			map.put("jobName", "123456");
			map.put("regDate", new Date());
			map.put("sendDate", new Date());
			map.put("msgType", "SMS");
			map.put("totalCnt", 100);
			db.exec(DB_NAMESPACE, "SHUTDOWN", map);

			h.connectDB("cfg/mybatis-hsqldb-dbagent.xml", 
				"hsqldb",
				//"jdbc:hsqldb:mem:mymemdb",
				"jdbc:hsqldb:file:hsqldb/testdb;shutdown=true",
				/*"jdbc:hsqldb:file:D:/tt/tt/test",*/
				"SA",
				""
				);
			System.out.println("DB접속 성공");
			db.exec(DB_NAMESPACE, "insert_msg_job", map);

			
			int cnt = (int) db.selectOne(DB_NAMESPACE, "select_job", null);
			System.out.println("CNT!!!!!!===================" + cnt);

			db.exec(DB_NAMESPACE, "SHUTDOWN", map);

			



		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void connectDB(String xmlFile, String xmlNS, String dbUrl, String userName, String passwd) throws Exception {

		DB_NAMESPACE = xmlNS;
		
		MybatisHandler.makeMybatisHandler(
			DB_NAMESPACE,
			dbUrl,
			userName,
			passwd,
			"cfg/mybatis-config.xml",
			xmlFile
			);
		db = MybatisHandler.getInstance(DB_NAMESPACE);
	}
}