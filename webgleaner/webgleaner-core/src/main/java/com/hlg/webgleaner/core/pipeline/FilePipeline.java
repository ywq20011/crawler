/**
 * 
 */
package com.hlg.webgleaner.core.pipeline;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

/**
 * 文件传输管道
 * @author linjx
 * @Date 2016年3月8日
 * @Version 1.0.0
 */
public class FilePipeline extends FilePersistentBase implements Pipeline {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public FilePipeline(String path) {
		setPath(path);
	}

	public FilePipeline() {
		
	}
	
	@Override
	public void process(ResultItems resultItems, Task task) {
		String path = this.path + PATH_SEPERATOR + task.getSite().getDomain() + PATH_SEPERATOR;
        try {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(
            		new FileOutputStream(getFile(path + "goods-html.txt")),resultItems.get("charset").toString()));
            printWriter.println(resultItems.get("html").toString());
            printWriter.close();
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
	}

}
