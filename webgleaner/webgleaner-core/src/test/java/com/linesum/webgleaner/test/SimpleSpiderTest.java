/**
 * 
 */
package com.linesum.webgleaner.test;

import org.junit.Test;

import com.hlg.webgleaner.core.downloader.HttpClientDownloader;
import com.hlg.webgleaner.core.pipeline.FilePipeline;
import com.hlg.webgleaner.core.processor.ExtractHtmlPageProcessor;

import us.codecraft.webmagic.Spider;

/**
 * 
 * @author linjx
 * @Date 2016年3月9日
 * @Version 1.0.0
 */
public class SimpleSpiderTest {
	
	@Test
	public void simpleSpider() {
		String startUrl = "https://lining.tmall.com/shop/view_shop.htm?spm=a230r.7195193.1997079397.2.uF5uKs&search=y";
		Spider spider = Spider.create(new ExtractHtmlPageProcessor(startUrl))
				.setDownloader(new HttpClientDownloader())
				.addPipeline(new FilePipeline("E:\\test\\spider"))
				.thread(1);
		spider.run();
	}

}
