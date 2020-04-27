package com.hlg.webgleaner.core.utils;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClientURI;
/**
 * 获取MongoTemplate工具类
 * 
 * @author yangwq
 * @Date 2016年8月24日
 */
@Component
public class MongoUtils {

	@Value(value="${mongo.uri}")
	private String mongoUri;
	
	private MongoTemplate template;
	
	public MongoUtils(){
		
	}
	
	/**
	 * 指定操作的数据库名称。
	 * @param dbName
	 * @return
	 */
	public  MongoTemplate getMongoTemplate(String db){
		if(template == null) {
			MongoClientURI uri = new MongoClientURI(mongoUri+db);
			try {
				template = new MongoTemplate( new SimpleMongoDbFactory(uri));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return template;
	}
	
	
}
