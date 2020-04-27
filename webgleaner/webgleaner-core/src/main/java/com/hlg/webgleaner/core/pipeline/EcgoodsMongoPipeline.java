package com.hlg.webgleaner.core.pipeline;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * Mongo存储管道
 * 
 * @author linjx
 * @Date 2016年3月21日
 * @Version 1.0.0
 */
public class EcgoodsMongoPipeline implements Pipeline {

	public static final String BRAND = "brands";
	public static final String SHOP = "shops";
	public static final String GOOD = "goods";

	public static final String COLLECTION_TYPE = "_type";
	public static final String RESULT_DOCS = "_result_doc";
	public static final String PREFIX = "_prefix";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	private String collectionName;

	private MongoTemplate template;
	
	public EcgoodsMongoPipeline(MongoTemplate template,String collectionName) {
		this.template = template;
		this.collectionName = collectionName;
	}
	
	@Override
	public void process(ResultItems resultItems, Task task) {
		List<?> resultDocs = resultItems.get(RESULT_DOCS);
		//MongoDao dao = SpringContextHelper.getBean(MongoDao.class);
		if (CollectionUtils.isNotEmpty(resultDocs)) {
			for(Object obj : resultDocs){
				template.save(obj,collectionName);
			}
			//dao.insertMany(resultDocs, collectionName);
		}
	}

}
