package com.hlg.webgleaner.core.proxy;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.hlg.webgleaner.core.utils.BeanUtils;
import com.hlg.webgleaner.core.utils.HttpClientUtils;
import com.hlg.webgleaner.core.utils.TemplateParser;

import us.codecraft.webmagic.proxy.ProxyIP;
import us.codecraft.webmagic.proxy.ProxyPool;

/**
 * http代理池 <br>
 * 默认配置文件为proxy-pool.properties<br>
 * 当前只能保证单机只有唯一proxyPool，暂时未实现分布式共享
 * @author linjx
 * @Date 2016年3月26日
 * @Version 1.0.0
 */
public class ProxyPoolContext {
	
	private static final Logger logger = LoggerFactory.getLogger(ProxyPoolContext.class);
	
	private static final ProxyPool proxyPool = (new ProxyPool(null)).enable(true);
	
	private static final ProxyPoolContext instance = new ProxyPoolContext();
	
	private static ResourceBundle bundle;
	
	static {
		bundle = ResourceBundle.getBundle("proxy-pool");
		//initKuaidailiVPS();  //初始化VPS代理
	}
	
	private ProxyPoolContext() {
	}
	
	public static ProxyPoolContext getInstance() {
		return instance;
	}
	
	public ProxyPool getProxyPool() {
		return proxyPool;
	}
	
	/**
	 * 通过API调用获取代理IP
	 * @param isHttps
	 * @return
	 */
	public void getProxyByDaili666API(boolean isHttps) {
		List<ProxyIP> proxyList = new ArrayList<ProxyIP>();
		RequestParams reqParams = new RequestParams();
		reqParams.setTid(bundle.getString("daili666.ip.proxy.tid"));
		reqParams.setNum(bundle.getString("daili666.ip.proxy.takeNum"));
		
		Map<String, Object> params = null;
		try {
			params = BeanUtils.convertBean2Map(reqParams);
		} catch (IllegalArgumentException e) {
			logger.error("bean to map error: ", e);
		} catch (IntrospectionException e) {
			logger.error("bean to map error: ", e);
		} catch (IllegalAccessException e) {
			logger.error("bean to map error: ", e);
		} catch (InvocationTargetException e) {
			logger.error("bean to map error: ", e);
		}
		
		String apiUrl = TemplateParser.replaceArgs(bundle.getString("daili666.ip.proxy.api.parttern"), params);
		if (isHttps) {
			apiUrl += "&protocol=https";
		}
		logger.info("request api: " + apiUrl);
		
		Map<String, Object> resultMap = HttpClientUtils.doGet(apiUrl);
		String[] items = StringUtils.split(String.valueOf(resultMap.get(HttpClientUtils.RESPONSE_CONTENT)), "\r\n");
		if (ArrayUtils.isNotEmpty(items)) {
			for (String item : items) {
				if (StringUtils.isNotBlank(item)) {
					String[] ipport = StringUtils.split(item, ":");
					if (ipport.length == 2) {
						ProxyIP proxy = new ProxyIP(ipport[0], Integer.valueOf(ipport[1]), isHttps);
						proxyList.add(proxy);
					}
				}
			}
		}
		String protocol = isHttps ? "https" : "http";
		logger.info(">>>> get " + protocol + " >> " + proxyList.size() + " proxy ips.");
		
		if (CollectionUtils.isNotEmpty(proxyList)) {
			proxyPool.addProxy(proxyList.toArray(new ProxyIP[] {}));
		}
	}
	
	/**
	 * 通过API调用获取代理IP
	 * @param isHttps
	 * @return
	 */
	public void getProxyByKuaidlAPI(boolean isHttps) {
		List<ProxyIP> proxyList = new ArrayList<ProxyIP>();
		
		Map<String, Object> params = Maps.newHashMap();
		params.put("tid", bundle.getString("kuaidaili.ip.proxy.tid"));
		params.put("num", bundle.getString("kuaidaili.ip.proxy.takeNum"));
		if (isHttps) {
			params.put("protocol", 2);
		} else {
			params.put("protocol", 1);
		}
		
		String apiUrl = TemplateParser.replaceArgs(bundle.getString("kuaidaili.ip.proxy.api.parttern"), params);
		logger.info("request api: " + apiUrl);
		
		Map<String, Object> resultMap = HttpClientUtils.doGet(apiUrl);
		String[] items = StringUtils.split(String.valueOf(resultMap.get(HttpClientUtils.RESPONSE_CONTENT)), "\r\n");
		if (ArrayUtils.isNotEmpty(items)) {
			for (String item : items) {
				if (StringUtils.isNotBlank(item)) {
					String[] ipport = StringUtils.split(item, ":");
					if (ipport.length == 2) {
						ProxyIP proxy = new ProxyIP(ipport[0], Integer.valueOf(ipport[1]), isHttps);
						proxyList.add(proxy);
					}
				}
			}
		}
		String protocol = isHttps ? "https" : "http";
		logger.info(">>>> get " + protocol + " >> " + proxyList.size() + " proxy ips.");
		
		if (CollectionUtils.isNotEmpty(proxyList)) {
			proxyPool.addProxy(proxyList.toArray(new ProxyIP[] {}));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void initKuaidailiVPS() {
		List<ProxyIP> proxyList = new ArrayList<ProxyIP>();
		
		Map<String, Object> params = Maps.newHashMap();
		params.put("tid", bundle.getString("kuaidaili.ip.proxy.tid"));
		String apiUrl = TemplateParser.replaceArgs(bundle.getString("kuaidaili.vps"), params);
		logger.info("request api: " + apiUrl);
		
		Map<String, Object> resultMap = HttpClientUtils.doGet(apiUrl);
		String[] items = StringUtils.split(String.valueOf(resultMap.get(HttpClientUtils.RESPONSE_CONTENT)), "\r\n");
		if (ArrayUtils.isNotEmpty(items)) {
			for (String item : items) {
				if (StringUtils.isNotBlank(item)) {
					String[] ipport = StringUtils.split(item, ":");
					if (ipport.length == 2) {
						ProxyIP httpsProxy = new ProxyIP(ipport[0], Integer.valueOf(ipport[1]), true); 
						ProxyIP httpProxy = new ProxyIP(ipport[0], Integer.valueOf(ipport[1]), false); 
						for (int i = 0; i < 10; i ++) {
							proxyList.add(httpsProxy);
							proxyList.add(httpProxy);
						}
					}
				}
			}
		}
		
		if (CollectionUtils.isNotEmpty(proxyList)) {
			proxyPool.addVpsProxy(200, proxyList.toArray(new ProxyIP[] {})); //重新使用时间间隔为0.5s
		}
	}
	
	public synchronized void getProxyByAPI(boolean isHttps) {
		try {
			Thread.sleep(1000);//保证不会太频繁的请求，否则会被api丢弃
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (StringUtils.equals("daili666", bundle.getString("api.switcher"))) {
			getProxyByDaili666API(isHttps);
		} else if (StringUtils.equals("onlyKuaiVps", bundle.getString("api.switcher"))) {
			//只使用快代理，则不再做操作
		} else {
			getProxyByKuaidlAPI(isHttps);
		}
	}

}
