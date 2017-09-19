package com.hlg.webgleaner.core.utils;

import com.mongodb.BasicDBObject;

import us.codecraft.webmagic.utils.IdUtils;

/**
 * 爬虫配置工具类
 * 
 * @author yangwq
 * @Date 2016年11月21日
 */
public class SpiderConfigUtils {

	/**
	 * 设置done,retryTimes,_id字段，_id字段采用IdUtils生成。如果你想自己生成_id字段，请在调用本方法后设置。
	 * @param doc
	 */
	public static void setDefaultField(BasicDBObject doc){
		doc.put("done", false);
		doc.put("retryTimes", 0);
		doc.put("_id", IdUtils.id());
	}
	
}
