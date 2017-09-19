package com.hlg.webgleaner.core.proxy;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hlg.webgleaner.core.utils.BeanUtils;
import com.hlg.webgleaner.core.utils.HttpClientUtils;
import com.hlg.webgleaner.core.utils.TemplateParser;

import us.codecraft.webmagic.proxy.ProxyIP;
import us.codecraft.webmagic.proxy.ProxyPool;

/**
 * 代理IP获取
 * @author linjx
 * @Date 2016年3月26日
 * @Version 1.0.0
 */
public class ProxyGetter {
	
	private static final Logger logger = LoggerFactory.getLogger(ProxyGetter.class);
	
	private static final ProxyPool proxyPool = new ProxyPool(null, false, "D:\\test\\httpProxy02.txt");
	
	private ProxyGetter() {}
	
	public static ProxyPool getProxyPool() {
		return proxyPool;
	}
	
	/**
	 * 文件读取，每行为ip:port
	 * @param filePath 文件路径
	 * @param isHttps 是否支持https
	 * @return
	 */
	public static void getProxyByFile(String filePath, boolean isHttps) {
		File file = new File(filePath);
		if (!file.exists()) {
			return ;
		}
		
		List<ProxyIP> proxyList = new ArrayList<ProxyIP>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (StringUtils.isBlank(line)) {
					continue;
				}
				String[] ipPort = StringUtils.split(line, ":");
				ProxyIP pip = new ProxyIP(ipPort[0], Integer.valueOf(ipPort[1]), isHttps);
				proxyList.add(pip);
			}
		} catch (FileNotFoundException e) {
			logger.error("init error: ", e); 
		} catch (NumberFormatException e) {
			logger.error("init error: ", e); 
		} catch (IOException e) {
			logger.error("init error: ", e); 
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error("file reader close error: ", e); 
				}
			}
		}
		
		if (CollectionUtils.isNotEmpty(proxyList)) {
			proxyPool.addProxy(proxyList.toArray(new ProxyIP[] {}));
		}
	} 
	
	
	/**
	 * API, @see http://www.daili666.net/api
	 */
	private static final String proxyUrl = "http://qsrdk.daili666api.com/ip/?foreign=only&exclude_ports=8088,8090,18186&filter=on&category=2&delay=5&tid=${tid}&num=${num}&operator=${operator}";
	/**
	 * 通过API调用获取代理IP
	 * @param isHttps
	 * @return
	 */
	public static void getProxyByAPI(boolean isHttps, int num, String tid) {
		List<ProxyIP> proxyList = new ArrayList<ProxyIP>();
		RequestParams reqParams = new RequestParams();
		reqParams.setTid(tid);
		reqParams.setNum(String.valueOf(num));
		
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
		
		String apiUrl = TemplateParser.replaceArgs(proxyUrl, params);
		if (isHttps) {
			apiUrl += "&protocol=https";
		}
		logger.info("request api: " + apiUrl);
		
		Map<String, Object> resultMap = HttpClientUtils.doGet(apiUrl);
		logger.info("get " + resultMap.size() + " proxy ips.");
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
		
		if (CollectionUtils.isNotEmpty(proxyList)) {
			proxyPool.addProxy(proxyList.toArray(new ProxyIP[] {}));
		}
	}
	
	
}
