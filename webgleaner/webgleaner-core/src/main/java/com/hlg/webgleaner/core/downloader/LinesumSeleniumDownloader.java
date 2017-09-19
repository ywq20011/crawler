/**
 * 
 */
package com.hlg.webgleaner.core.downloader;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

/**
 * 添加了重试机制和浏览器模拟操作
 * @author linjx
 * @Date 2016年3月16日
 * @Version 1.0.0
 */
public class LinesumSeleniumDownloader implements Downloader,Closeable {

	// WebDriverPool
	private WebDriverPool webDriverPool = WebDriverPool.getInstance();

	private Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unused")
	private int poolSize = 1;

	private String script = null;
	
	private Long sleepTime = null;
	
	public LinesumSeleniumDownloader(String scriptName,Long sleepTime) throws IOException{
		Properties prop = new Properties();
		String path = this.getClass().getClassLoader().getResource("").getPath();
		InputStream in = new FileInputStream(path+"/chromeDriver.properties");
		prop.load(in);
		this.script = prop.getProperty(scriptName); 
		this.sleepTime = sleepTime;
	}
	
	 public LinesumSeleniumDownloader() { }
	
	// TODO 改成配置文件
	@SuppressWarnings("unused")
	private static final String DRIVER_PHANTOMJS = "phantomjs";
	@SuppressWarnings("unused")
	private static final String PHANTOMJS_DRIVER_KEY = "phantomjs.binary.path";
	private static final String CHROME_DRIVER_KEY = "webdriver.chrome.driver";

	public LinesumSeleniumDownloader(String chromeDriverPath) {
		// System系统属性
		System.getProperties().setProperty(CHROME_DRIVER_KEY, chromeDriverPath);

	}

	public LinesumSeleniumDownloader(String chromeDriverPath, int hitRatio) {
		// System系统属性
		System.getProperties().setProperty(CHROME_DRIVER_KEY, chromeDriverPath);
	}

	public LinesumSeleniumDownloader setSleepTime(Long sleepTime) {
		this.sleepTime = sleepTime;
		return this;
	}

	@Override
	public Page download(Request request, Task task) throws Exception {
		
		Site site = task.getSite();
	    if((Integer)request.getExtra(Request.CYCLE_TRIED_TIMES) == null){
        	request.putExtra(Request.CYCLE_TRIED_TIMES, 0);
        }
        if((Integer)request.getExtra(Request.CYCLE_TRIED_TIMES) > site.getCycleRetryTimes()){//超过重试次数，return null。否则往下走。
        	logger.info("retry times exceeded..."); //判断是否重试.
        	return null;
        }
		checkInit();
		WebDriver webDriver;
		Page page = null;//要返回的page对象；
		webDriver = webDriverPool.get(WebDriverPool.DRIVER_CHROME);
		WebDriver.Options manage = webDriver.manage();
		if (site.getCookies() != null) {
			for (Map.Entry<String, String> cookieEntry : site.getCookies().entrySet()) {
				Cookie cookie = new Cookie(cookieEntry.getKey(), cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		logger.info("downloading page " + request.getUrl());
		webDriver.get(request.getUrl());
		//js模拟操作
		if(script != null){
			logger.info("执行javaScript脚本...");
			((RemoteWebDriver) webDriver).executeScript(script, "");
			logger.info("等待"+sleepTime/1000+"s...");
			Thread.sleep(sleepTime);
		}
		
		//鼠标键盘模拟操作
		simulate(request,webDriver);
	
		WebElement webElement = webDriver.findElement(By.xpath("/html"));
		String content = webElement.getAttribute("outerHTML");
		page = new Page();
		page.setRawText(content);
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		// charset
		List<WebElement> metaElems = webDriver.findElements(By.tagName("meta"));
		if (CollectionUtils.isNotEmpty(metaElems)) {
			for (WebElement meta : metaElems) {
				if (StringUtils.equals("Content-Type", meta.getAttribute("http-equiv"))) {
					String contentType = meta.getAttribute("content");
					if (StringUtils.isNotBlank(contentType) && contentType.indexOf("charset") > -1) {
						String charsetNV = StringUtils.substring(contentType, contentType.indexOf("charset="));
						page.putField("charset", charsetNV.split("=")[1]);
					}
				}
			}
		}
		webDriverPool.returnToPool(webDriver);
		return page;
	}

	protected  void simulate(Request request, WebDriver webDriver) throws InterruptedException{};
	
	private void checkInit() {
		/*
		 * if (webDriverPool == null) { synchronized { webDriverPool =
		 * WebDriverPool.getInstance(); } }
		 */
	}
	@Override
	public void setThread(int thread) {
		this.poolSize = thread;
	}

	@Override
	public void close() throws IOException {
//		if (null != webDriverPool) {
//			webDriverPool.closeAll();
//		}
	}

}
