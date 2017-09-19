package com.hlg.webgleaner.core.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.ResourceUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;


/**
 * @author code4crafter@gmail.com <br>
 *         Date: 13-7-26 <br>
 *         Time: 下午1:41 <br>
 * @update linjx
 * @Date 2016年3月16日
 * @Version 1.0.0
 */
public class WebDriverPool {
	private Logger logger = Logger.getLogger(getClass());

	private final static int DEFAULT_CAPACITY = 10;

	private final int capacity;

	private final static int STAT_RUNNING = 1;

	private final static int STAT_CLODED = 2;

	//private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

	
	public static final String DRIVER_FIREFOX = "firefox";
	public static final String DRIVER_CHROME = "chrome";
	public static final String DRIVER_PHANTOMJS = "phantomjs";

	protected static Properties sConfig;
	protected static DesiredCapabilities sCaps;

	/**
	 * Configure the GhostDriver, and initialize a WebDriver instance. This part
	 * of code comes from GhostDriver.
	 * https://github.com/detro/ghostdriver/tree/master/test/java/src/test/java/ghostdriver
	 * Note：当前只针对PhantomJSDriver做了配置
	 * @author bob.li.0718@gmail.com
	 * @throws IOException
	 */
	
	public WebDriver configure() throws IOException {
		WebDriver mDriver = null;
		sCaps = new DesiredCapabilities();
		sCaps.setJavascriptEnabled(true);
		sCaps.setCapability("takesScreenshot", true);
		String driver = DRIVER_CHROME;
		// Fetch PhantomJS-specific configuration parameters
		if (driver.equals(DRIVER_PHANTOMJS)) {
			// "phantomjs_exec_path"
			if (sConfig.getProperty("phantomjs_exec_path") != null) {
				sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
						sConfig.getProperty("phantomjs_exec_path"));
			} else {
				throw new IOException(String.format("Property '%s' not set!",
						PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY));
			}
			// "phantomjs_driver_path"
			if (sConfig.getProperty("phantomjs_driver_path") != null) {
				System.out.println("Test will use an external GhostDriver");
				sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY,
						sConfig.getProperty("phantomjs_driver_path"));
			} else {
				System.out.println("Test will use PhantomJS internal GhostDriver");
			}
		}

		// Disable "web-security", enable all possible "ssl-protocols" and "ignore-ssl-errors" for PhantomJSDriver
		ArrayList<String> cliArgsCap = new ArrayList<String>();
		cliArgsCap.add("--web-security=false");
		cliArgsCap.add("--ssl-protocol=any");
		cliArgsCap.add("--ignore-ssl-errors=true");
		cliArgsCap.add("--load-images=yes"); //不加载图片
		sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
		//设置user-agent
//		sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", 
//				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36");

		// Control LogLevel for GhostDriver, via CLI arguments
		sCaps.setCapability(
				PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
				new String[] { "--logLevel=" + (sConfig.getProperty("phantomjs_driver_loglevel") != null 
					? sConfig.getProperty("phantomjs_driver_loglevel") : "INFO") });
		// Start appropriate Driver
		if (isUrl(driver)) {
			sCaps.setBrowserName("phantomjs");
			mDriver = new RemoteWebDriver(new URL(driver), sCaps);
		} else if (driver.equals(DRIVER_FIREFOX)) {
			sCaps.setBrowserName("firefox");
			System.getProperties().setProperty("webdriver.firefox.bin", "");
			mDriver = new FirefoxDriver(sCaps);
		} else if (driver.equals(DRIVER_CHROME)) {
			Map<String, Object> contentSettings = new HashMap<String, Object>();
			contentSettings.put("images", 0);
			Map<String, Object> preferences = new HashMap<String, Object>();
			preferences.put("profile.default_content_settings", contentSettings);
			ChromeOptions options = new ChromeOptions();
			options.setBinary(sConfig.getProperty("chrome.real.path")); 
			sCaps.setCapability("chrome.prefs", preferences);
			sCaps.setCapability(ChromeOptions.CAPABILITY, options);
			System.getProperties().setProperty("webdriver.chrome.driver", sConfig.getProperty("chrome.driver.path"));
			mDriver = new ChromeDriver(sCaps);
		} else if (driver.equals(DRIVER_PHANTOMJS)) {
			/*sCaps.setBrowserName("chrome");
			sCaps.setVersion("40.0.2214.94");
			sCaps.setPlatform(Platform.WINDOWS);*/
			mDriver = new PhantomJSDriver(sCaps);
		}
		
		return mDriver;
	}
	
	/**
	 * Configure the GhostDriver, and initialize a WebDriver instance. This part
	 * of code comes from GhostDriver.
	 * https://github.com/detro/ghostdriver/tree/master/test/java/src/test/java/ghostdriver
	 * Note：当前只针对PhantomJSDriver做了配置
	 * @author bob.li.0718@gmail.com
	 * @throws IOException
	 */
	public WebDriver configure(String driver) throws IOException {
		WebDriver mDriver = null;
		// Prepare capabilities
		sCaps = new DesiredCapabilities();
		sCaps.setJavascriptEnabled(true);
		sCaps.setCapability("takesScreenshot", false);
		// Fetch PhantomJS-specific configuration parameters
		if (driver.equals(DRIVER_PHANTOMJS)) {
			// "phantomjs_exec_path"
			if (sConfig.getProperty("phantomjs_exec_path") != null) {
				sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
						sConfig.getProperty("phantomjs_exec_path"));
			} else {
				throw new IOException(String.format("Property '%s' not set!",
						PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY));
			}
			// "phantomjs_driver_path"
			if (sConfig.getProperty("phantomjs_driver_path") != null) {
				System.out.println("Test will use an external GhostDriver");
				sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY,
						sConfig.getProperty("phantomjs_driver_path"));
			} else {
				System.out.println("Test will use PhantomJS internal GhostDriver");
			}
		}

		// Disable "web-security", enable all possible "ssl-protocols" and "ignore-ssl-errors" for PhantomJSDriver
		ArrayList<String> cliArgsCap = new ArrayList<String>();
		cliArgsCap.add("--web-security=false");
		cliArgsCap.add("--ssl-protocol=any");
		cliArgsCap.add("--ignore-ssl-errors=true");
		cliArgsCap.add("--load-images=yes"); //不加载图片
		sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
		//设置user-agent
//		sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", 
//				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36");
		// Control LogLevel for GhostDriver, via CLI arguments
		sCaps.setCapability(
				PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
				new String[] { "--logLevel=" + (sConfig.getProperty("phantomjs_driver_loglevel") != null 
					? sConfig.getProperty("phantomjs_driver_loglevel") : "INFO") });
		
		// Start appropriate Driver
		if (isUrl(driver)) {
			sCaps.setBrowserName("phantomjs");
			mDriver = new RemoteWebDriver(new URL(driver), sCaps);
		} else if (driver.equals(DRIVER_FIREFOX)) {
			sCaps.setBrowserName("firefox");
			System.getProperties().setProperty("webdriver.firefox.bin", "");
			mDriver = new FirefoxDriver(sCaps);
		} else if (driver.equals(DRIVER_CHROME)) {
			Map<String, Object> contentSettings = new HashMap<String, Object>();
			contentSettings.put("images", 0);
			Map<String, Object> preferences = new HashMap<String, Object>();
			preferences.put("profile.default_content_settings", contentSettings);
			sCaps.setCapability("chrome.prefs", preferences);
			ChromeOptions options = new ChromeOptions();
			options.setBinary(sConfig.getProperty("chrome.real.path"));//TODO 配置文件，实际chrome程序路径 
			sCaps.setCapability("chrome.prefs", preferences);
			sCaps.setCapability(ChromeOptions.CAPABILITY, options);
			//--------------------
			System.getProperties().setProperty("webdriver.chrome.driver", sConfig.getProperty("chrome.driver.path"));//驱动路径
			mDriver = new ChromeDriver(sCaps);
		} else if (driver.equals(DRIVER_PHANTOMJS)) {
			/*sCaps.setBrowserName("chrome");
			sCaps.setVersion("40.0.2214.94");
			sCaps.setPlatform(Platform.WINDOWS);*/
			mDriver = new PhantomJSDriver(sCaps);
		}
		
		return mDriver;
	}

	/**
	 * check whether input is a valid URL
	 * 
	 * @author bob.li.0718@gmail.com
	 * @param urlString urlString
	 * @return true means yes, otherwise no.
	 */
	private boolean isUrl(String urlString) {
		try {
			new URL(urlString);
			return true;
		} catch (MalformedURLException mue) {
			return false;
		}
	}

	/**
	 * store webDrivers created
	 */
	private List<WebDriver> webDriverList = Collections
			.synchronizedList(new ArrayList<WebDriver>());

	/**
	 * store webDrivers available
	 */
	private BlockingDeque<WebDriver> innerQueue = new LinkedBlockingDeque<WebDriver>();

	private WebDriverPool(int capacity) {
		sConfig = new Properties();
		try {
			sConfig.load(new FileInputStream(new File("webKit.properties")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.capacity = capacity;
	}

	private WebDriverPool() {
		this(DEFAULT_CAPACITY);
	}
	
	private static WebDriverPool driverPool = new WebDriverPool();
	public static WebDriverPool getInstance() {
		return driverPool;
	}

	/**
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public WebDriver get() throws InterruptedException {
		//checkRunning();
		WebDriver poll = innerQueue.poll();
		if (poll != null) {
			return poll;
		}
		//if (webDriverList.size() < capacity) {
		if (innerQueue.size() < capacity) {  //不使用webDriverList，因为如果不断删除driver，没有正常传回webDriver,jiu 
			synchronized (webDriverList) {
//				if (webDriverList.size() < capacity) {
			if (innerQueue.size() < capacity) {
					// add new WebDriver instance into pool
					try {
						WebDriver mDriver = configure();
						innerQueue.add(mDriver);
						webDriverList.add(mDriver);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
		WebDriver wd = innerQueue.take();
		if (((int) (Math.random()*100)) % 3 == 0) {
			//wd.close();
		} else {
			wd.manage().deleteAllCookies();
		}
		return wd;
	}
	
	/**
	 * 获取WebDriver实例
	 * @param driverType driver类型
	 * @param configFile 配置文件
	 * @return
	 * @throws InterruptedException
	 */
	public WebDriver get(String driverType) throws InterruptedException {
		//TODO driverType使用Enum
		//checkRunning();
		WebDriver poll = innerQueue.poll();
		
		if (poll != null) {
			return poll;
		}
		if (innerQueue.size() < capacity) {
			synchronized (webDriverList) {
				if (innerQueue.size() < capacity) {
					// add new WebDriver instance into pool
					try {
						WebDriver mDriver = configure(driverType);
						innerQueue.add(mDriver);
						webDriverList.add(mDriver);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
		WebDriver wd = innerQueue.take();
		if (((int) (Math.random()*100)) % 3 == 0) {
			//wd.close();
		} else {
			wd.manage().deleteAllCookies();
		}
		return wd;
	}
	
	
	

	public void returnToPool(WebDriver webDriver) {
		//checkRunning();
		//webDriver.quit();
		innerQueue.add(webDriver);
	}

	//protected void checkRunning() {
	//	if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
	//		throw new IllegalStateException("Already closed!");
	//	}
	//}

	public void closeAll() {
	//	boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
	//	if (!b) {
	//		throw new IllegalStateException("Already closed!");
	//	}
		synchronized (webDriverList) {
			if (CollectionUtils.isNotEmpty(webDriverList)) {
				for (WebDriver webDriver : webDriverList) {
					logger.info("Quit webDriver" + webDriver);
					webDriver.quit();
					webDriver = null;
				}
				
				webDriverList = Collections.synchronizedList(new ArrayList<WebDriver>());
				innerQueue = new LinkedBlockingDeque<WebDriver>();
				
				
			}
		}
	}

}
