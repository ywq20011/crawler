package com.hlg.webgleaner.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpClient工具类
 * 
 * @author linjx
 * @Date 2016年2月17日
 * @Version 1.0.0
 */
public class HttpClientUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtils.class);

	private static PoolingHttpClientConnectionManager poolingConnectionMgr;
	public static final String UTF_8_CHARSET = "UTF-8";
	public static final String STATUS_CODE = "status";
	public static final String RESPONSE_CONTENT = "response";

	/*
	 * static { RequestConfig config = RequestConfig.custom()
	 * .setConnectTimeout(600000).setSocketTimeout(600000).build(); httpClient =
	 * HttpClientBuilder.create().setDefaultRequestConfig(config).build(); }
	 */
	private synchronized static void init() {
		if (poolingConnectionMgr == null) {
			poolingConnectionMgr = new PoolingHttpClientConnectionManager();
			poolingConnectionMgr.setMaxTotal(50);// 整个连接池最大连接数
			poolingConnectionMgr.setDefaultMaxPerRoute(5);// 每路由最大连接数，默认值是2
		}
	}

	/**
	 * 通过连接池获取HttpClient
	 * 
	 * @return
	 */
	private static CloseableHttpClient getHttpClient() {
		init();
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(600000).setSocketTimeout(600000).build();// 标准Cookie策略
		return HttpClients.custom().setConnectionManager(poolingConnectionMgr).setDefaultRequestConfig(requestConfig)// 设置cookie策略
				.build();
	}

	public static Map<String, Object> doGet(String url) {
		return doGet(url, null, UTF_8_CHARSET);
	}

	public static Map<String, Object> doGet(String url, Map<String, String> params) {
		return doGet(url, params, UTF_8_CHARSET);
	}

	public static Map<String, Object> doPost(String url, Map<String, String> params) {
		return doPost(url, params, UTF_8_CHARSET);
	}

	/**
	 * GET 请求
	 * 
	 * @param url
	 *            url地址
	 * @param params
	 *            请求参数
	 * @param charset
	 *            编码
	 * @return
	 */
	public static Map<String, Object> doGet(String url, Map<String, String> params, String charset) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);

		try {
			URIBuilder ub = new URIBuilder(new URI(url));
			if (MapUtils.isNotEmpty(params)) {
				List<NameValuePair> pairs = convertParams2NVP(params);
				ub.setParameters(pairs);
			}

			HttpGet get = new HttpGet(ub.build());
			resultMap = getHttpResponse(get, charset);
		} catch (Exception e) {
			LOGGER.error("请求失败[{}]", e);
		}
		return resultMap;
	}

	/**
	 * get
	 * 
	 * @param url
	 * @param paramsString
	 *            已经拼接好的请求参数
	 * @param charset
	 * @return
	 */
	public static Map<String, Object> doGetByParamsString(String url, String paramsString, String charset) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);
		if (StringUtils.isBlank(charset)) {
			charset = UTF_8_CHARSET;
		}

		try {
			if (StringUtils.isNotBlank(paramsString)) {
				url = url + "?" + paramsString;
			}

			HttpGet get = new HttpGet(new URI(url));
			resultMap = getHttpResponse(get, charset);
		} catch (Exception e) {
			LOGGER.error("请求失败[{}]", e);
		}
		return resultMap;
	}

	/**
	 * POST请求方式，返回值
	 * 
	 * @param url
	 * @param params
	 * @param charset
	 * @return
	 */
	public static Map<String, Object> doPost(String url, Map<String, String> params, String charset) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);

		try {
			HttpPost post = new HttpPost(url);
			if (MapUtils.isNotEmpty(params)) {
				List<NameValuePair> pairs = convertParams2NVP(params);
				post.setEntity(new UrlEncodedFormEntity(pairs));
			}
			resultMap = getHttpResponse(post, charset);
		} catch (Exception e) {
			LOGGER.error("请求失败[{}]", e);
		}
		return resultMap;
	}

	/**
	 * 获取响应内容
	 * 
	 * @param request
	 * @param charset
	 * @return
	 */
	private static Map<String, Object> getHttpResponse(HttpRequestBase request, String charset) {
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);

		CloseableHttpResponse response;
		try {
			response = getHttpClient().execute(request);
			int status = response.getStatusLine().getStatusCode();
			String result = null;
			if (status != 200) {
				LOGGER.error("The response status code is not 200.");
			} else {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					result = EntityUtils.toString(entity, Charset.forName(charset));
				}
				EntityUtils.consume(entity);
			}
			resultMap.put(STATUS_CODE, status);
			resultMap.put(RESPONSE_CONTENT, result);
			request.abort();
			response.close();
		} catch (ClientProtocolException e) {
			LOGGER.error("request error[{}]", e);
		} catch (IOException e) {
			LOGGER.error("request error[{}]", e);
		}

		return resultMap;
	}

	/**
	 * 将参数转换为NameValuePair组
	 * 
	 * @param params
	 * @return
	 */
	private static List<NameValuePair> convertParams2NVP(Map<String, String> params) {
		List<NameValuePair> pairs = null;
		if (MapUtils.isNotEmpty(params)) {
			pairs = new ArrayList<NameValuePair>(params.size());
			for (Entry<String, String> entry : params.entrySet()) {
				String value = entry.getValue();
				if (value != null) {
					pairs.add(new BasicNameValuePair(entry.getKey(), value));
				}
			}
		}
		return pairs;
	}

	public static String doGet(String url, String referer) {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
		RequestConfig config = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
		get.setConfig(config);
		get.addHeader("referer", referer);
		get.addHeader("user-agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
		get.addHeader("origin", "https://www.chuangkit.com");
		get.addHeader("accept", "application/json, text/plain, */*");
		get.addHeader("accept-encoding", "gzip, deflate, br");
		get.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		CloseableHttpResponse response = null;
		try {
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String result = null;
		try {
			result = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;

	}

	public static String doGetWithHeader(String url, Header[] headers) {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpGet get = new HttpGet(url);
		get.setHeaders(headers);
		CloseableHttpResponse response = null;
		try {
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String result = null;
		try {
			result = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;

	}

	public static String curlGet(String url) {
		String result = "";
		String[] cmds = { "curl", "-s", url };
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.redirectErrorStream(true);
		Process p;
		try {
			p = pb.start();
			BufferedReader br = null;
			String line = null;

			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = br.readLine()) != null) {
				result = line;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static String driverGet(String url) {
		String pageSource = "";
		WebDriver driver = null;
		try {
			ChromeOptions options = new ChromeOptions();
			// options.setBinary("/Applications/Google Chrome 2.app/Contents/MacOS/Google
			// Chrome");
			System.setProperty("webdriver.chrome.driver", "/data/chuangkit/chromedriver");
			options.addArguments("--headless");// headless mode
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("start-maximized"); // open Browser in maximized mode
			options.addArguments("disable-infobars"); // disabling infobars
			options.addArguments("--disable-extensions"); // disabling extensions
			driver = new ChromeDriver(options);
			driver.get(url);
			
			pageSource = driver.getPageSource().replace("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head></head><body><pre style=\"word-wrap: break-word; white-space: pre-wrap;\">", "").replace("</pre></body></html>", "");
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
		return pageSource;
	}

	public static String doGetWithCharSet(String url, String referer, String charSet) {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
		get.addHeader("referer", referer);
		CloseableHttpResponse response = null;
		try {
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String result = null;
		try {
			result = EntityUtils.toString(response.getEntity(), charSet);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;

	}

	/**
	 * 使用post带上json
	 * 
	 * @param url
	 * @param json
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> doPostWithJSON(String url, String json, String referer) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);

		try {
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
			// 将JSON进行UTF-8编码,以便传输中文
			StringEntity se = new StringEntity(json, UTF_8_CHARSET);
			se.setContentType("application/json");
			httpPost.setEntity(se);
			resultMap = getHttpResponse(httpPost, UTF_8_CHARSET);
		} catch (Exception e) {
			LOGGER.error("请求失败[{}]", e);
		}
		return resultMap;
	}
	
	
	
	public static Map<String, Object> doPostWithJSONAndCookie(String url, String json, String referer,String cookie) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>(2);
		resultMap.put(STATUS_CODE, 0);
		resultMap.put(RESPONSE_CONTENT, null);

		try {
			HttpPost httpPost = new HttpPost(url);
			
			
			httpPost.setHeader("cookie",cookie);
			httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
			// 将JSON进行UTF-8编码,以便传输中文
			StringEntity se = new StringEntity(json, UTF_8_CHARSET);
			se.setContentType("application/json");
			httpPost.setEntity(se);
			resultMap = getHttpResponse(httpPost, UTF_8_CHARSET);
		} catch (Exception e) {
			LOGGER.error("请求失败[{}]", e);
		}
		return resultMap;
	}
	
	
	
	

	public static void main(String[] args) {

		Date date = new Date();
		System.out.println(date.getTime());

	}

}
