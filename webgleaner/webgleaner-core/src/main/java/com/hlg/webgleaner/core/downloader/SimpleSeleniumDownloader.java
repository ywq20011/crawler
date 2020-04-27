package com.hlg.webgleaner.core.downloader;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

/**
 * 简易driver，每次都初始化driver，使用后关闭。
 * 
 * @author yangwq
 *
 */
public abstract class SimpleSeleniumDownloader implements Downloader {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String driverType;

	private String driverPath;

	private boolean headless;
	
	private  Map<String, Object> prefs;
	
	public SimpleSeleniumDownloader(String driverType, String driverPath) {
		this.driverType = driverType;
		this.driverPath = driverPath;
	}
	
	public SimpleSeleniumDownloader(String driverType, String driverPath , boolean headless , Map<String,Object> prefs) {
		
		this.driverType = driverType;
		this.driverPath = driverPath;
		this.headless = headless;
		this.prefs = prefs;
		
	}
	

	@Override
	public Page download(Request request, Task task) throws Exception {
		
		// 实例化driver
		Page page = new Page();
		
		WebDriver driver = null;
		try {
		if (driverType.equalsIgnoreCase("phantomjs")) {
			System.setProperty("phantomjs.binary.path", driverPath);
			DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
			driver = new PhantomJSDriver(desiredCapabilities);
		} else if (driverType.equalsIgnoreCase("chromedriver")) {
			  ChromeOptions options = new ChromeOptions();
	 // options.setBinary("/Applications/Google Chrome 2.app/Contents/MacOS/Google Chrome");
		        System.setProperty("webdriver.chrome.driver", driverPath);
		        		options.addArguments("--headless");// headless mode
		        options.addArguments("--no-sandbox");
		        options.addArguments("--disable-dev-shm-usage");
		        options.addArguments("start-maximized"); // open Browser in maximized mode
		        options.addArguments("disable-infobars"); // disabling infobars
		        options.addArguments("--disable-extensions"); // disabling extensions
		        if(prefs != null) {
		        		options.setExperimentalOption("prefs", prefs);
		        }
			driver = new ChromeDriver(options);
		}

		Site site = task.getSite();
		if ((Integer) request.getExtra(Request.CYCLE_TRIED_TIMES) == null) {
			request.putExtra(Request.CYCLE_TRIED_TIMES, 0);
		}
		if ((Integer) request.getExtra(Request.CYCLE_TRIED_TIMES) > site.getCycleRetryTimes()) {// 超过重试次数，return
																								// null。否则往下走。
			logger.info("retry times exceeded..."); // 判断是否重试.
			return null;
		}
		
		WebDriver.Options manage = driver.manage();
		if (site.getCookies() != null) {
			for (Map.Entry<String, String> cookieEntry : site.getCookies().entrySet()) {
				Cookie cookie = new Cookie(cookieEntry.getKey(), cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		logger.info("downloading page " + request.getUrl());
		driver.get(request.getUrl());
		//js模拟操作
		
		//鼠标键盘模拟操作
		simulate(request,driver);
	
		WebElement webElement = driver.findElement(By.xpath("/html"));
		String content = webElement.getAttribute("outerHTML");
		page = new Page();
		page.setRawText(content);
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		// charset
		List<WebElement> metaElems = driver.findElements(By.tagName("meta"));
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
		
		}finally {
			if(driver!=null) {
				driver.quit();
			}
		}
		
		return page;
		
	}

	protected abstract void simulate(Request request, WebDriver driver) ;
	

	@Override
	public void setThread(int threadNum) {

	}

}
