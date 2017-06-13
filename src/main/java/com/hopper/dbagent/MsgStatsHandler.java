package com.hopper.dbagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.io.File;

import com.hopper.dbagent.vo.MessageVO;
import com.hopper.dbagent.vo.MessageStatsVO;

import java.text.SimpleDateFormat;

import java.util.Date;

public class MsgStatsHandler {

	private MybatisHandler db = null;

	private static final Logger log = LoggerFactory.getLogger(MsgStatsHandler.class);
	private String DB_NAMESPACE = "";

	public MsgStatsHandler(MybatisHandler db, String namespace) {
    this.db = db;
    DB_NAMESPACE = namespace;
	}

	public void update(Date sendDate, String msgType, int sentFail, int succ, int timeout, int invalid) {
		update(new MessageStatsVO(sendDate, msgType, sentFail, succ, timeout, invalid));
	}

	public void update(final MessageStatsVO statVo) {

		for (int i=0; i < 2; ++i) {
			try {
				int updateCnt = db.exec(DB_NAMESPACE, "update_msg_stats", statVo);
				if (updateCnt == 0) {
					db.exec(DB_NAMESPACE, "insert_msg_stats", statVo);
				}
				return;
			}
			catch(DBException ex) {
				if (ex.getCommonDBErrorCode() == DBException.Code.TABLE_NOT_EXIST) {
					try {
						db.exec(DB_NAMESPACE, "create_msg_stats", null);
					}
					catch(Exception exce) {
						log.warn(exce.toString());
					}
				}
				else {
					log.warn(StackTrace.toString(ex));
					return;
				}
			}
			catch(Exception ex) {
				log.warn(StackTrace.toString(ex));
			}
		}
	}
}