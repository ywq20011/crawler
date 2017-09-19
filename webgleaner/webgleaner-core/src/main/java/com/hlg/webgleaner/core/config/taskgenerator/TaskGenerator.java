package com.hlg.webgleaner.core.config.taskgenerator;

import org.springframework.data.mongodb.core.MongoTemplate;
/**
 * 任务生成接口
 * 
 * @author yangwq
 * @Date 2016年12月28日
 */
public interface TaskGenerator {

	public void generate(MongoTemplate template);
	
}
