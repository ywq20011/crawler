package com.hlg.webgleaner.core.scheduler;

import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.BasicDBObject;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.scheduler.Scheduler;

/**
 * 电商专用调度器，使用Mongo实现的队列调度器<br>
 * 主要包含额外包含了品牌ID、店铺ID等
 * 
 * @author linjx
 * @Date 2016年3月24日
 * @Version 1.0.0
 */
public class EcMongoQueueScheduler implements Scheduler {

	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
	public static final String REQUEST_URL = "url";
	public static final String DOC_ID = "_id";
	public static final String VERSION = "version";
	public static final String DOCUMENT = "_document_";
	public static final String RETRY_TIMES = "retryTimes";

	private MongoTemplate template;
	private String collName;
	
	public EcMongoQueueScheduler() {
		super();
	}

	public EcMongoQueueScheduler(MongoTemplate template, String collName) {
		this.template = template;
		this.collName = collName;
	}

	@Override
	public void push(Request request, Task task) {
		BasicDBObject doc = null;
		if (null != request.getExtra(DOCUMENT)) { // 改由request传递，保持格式一致
			doc = (BasicDBObject) request.getExtra(DOCUMENT);
		} else if (StringUtils.isNotBlank(request.getUrl())) {
			doc = new BasicDBObject();
		}
		if (null != doc) {
			doc.put("url", request.getUrl());
			doc.put(Request.CYCLE_TRIED_TIMES, request.getExtra(Request.CYCLE_TRIED_TIMES));
			doc.put("done", false);//未处理
			template.save(doc, collName);
			
		}
	}

	@Override
	public Request poll(Task task) { // 可以用于分片分任务
		//改为findandUpdate，，，查出一条，并修改字段done字段
		BasicDBObject doc = template.findAndModify(new Query(Criteria.where("done").is(false)), Update.update("done", true), BasicDBObject.class, collName);
		Request request = null;// 封装成request返回
		if (null != doc) {
			request = new Request(doc.getString("url"));
			request.putExtra(Request.CYCLE_TRIED_TIMES, doc.get(Request.CYCLE_TRIED_TIMES));
			request.putExtra(DOCUMENT, doc);
		}
		return request;
	}

//	/**
//	 * 关闭
//	 */
//	public void close() {
//		mongoClient.close();
//		System.out.println("close down mongo client ...");
//	}

}
