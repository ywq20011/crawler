package com.hlg.webgleaner.core.downloader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.hlg.webgleaner.core.proxy.ProxyPoolContext;
import com.hlg.webgleaner.core.utils.UserAgentContext;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.HttpClientGenerator;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.HttpConstant.Header;
import us.codecraft.webmagic.utils.UrlUtils;


/**
 * 基于HttpClient的下载器，属于基本下载器
 * The http downloader based on HttpClient.
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 * @update linjx@linesum.com <br>
 */
@ThreadSafe
public class HttpClientDownloader extends AbstractDownloader implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientDownloader.class);
    
    public static final String CHARSET = "_charset";
    public static final String CONTENT = "_content";
    
    private UserAgentContext userAgent;
    private int hitRatio = 10;  //使用本地IP的命中率，百分比。只能大于0
    
    public HttpClientDownloader() {}
    
    public HttpClientDownloader(int hitRatio) {
    	if (hitRatio > 0) {
    		this.hitRatio = hitRatio;
    	}
    }

    /**
     * httpClients集合，使用并发Map，减少反复创建HttpClient
     */
    private final ConcurrentMap<String, CloseableHttpClient> httpClients = new ConcurrentHashMap<String, CloseableHttpClient>();

    private HttpClientGenerator httpClientGenerator = new HttpClientGenerator();

    private CloseableHttpClient getHttpClient(Site site) {
        if (site == null || StringUtils.isBlank(site.getDomain())) {
            return httpClientGenerator.getClient(site);
        }
        String domain = site.getDomain();
        CloseableHttpClient httpClient = httpClients.get(domain);
        if (httpClient == null) {//TODO 优化
            httpClient = httpClientGenerator.getClient(site);
            httpClients.putIfAbsent(domain, httpClient);
        }
        return httpClient;
    }

    @Override
    public Page download(Request request, Task task) throws Exception{
        Site site = null;
        if (task != null) {
            site = task.getSite();
        }
        Set<Integer> acceptStatCode; //可接受状态码
        String charset = null; //编码
        Map<String, String> headers = null; //http头部信息
        if (site != null) {
            acceptStatCode = site.getAcceptStatCode();
            charset = site.getCharset();
            headers = site.getHeaders();
            //FIXME userAgent
            if (StringUtils.isBlank(site.getUserAgent()) && null != userAgent) {     	
            	headers.put(Header.USER_AGENT, userAgent.getRotationUserAgent());  
            	logger.info("use userAgent: {}",headers.get(Header.USER_AGENT));
            }
        } else {
            acceptStatCode = Sets.newHashSet(200);
        }
       
        // 重试机制
        if((Integer)request.getExtra(Request.CYCLE_TRIED_TIMES) == null){
        	request.putExtra(Request.CYCLE_TRIED_TIMES, 0);
        }
        //超过重试次数，return null。否则往下走。
        if((Integer)request.getExtra(Request.CYCLE_TRIED_TIMES) > site.getCycleRetryTimes()){
        	logger.info("retry times exceeded..."); //判断是否重试.
        	return null;
        }
        //开始下载页面
        logger.info("downloading page {}", request.getUrl());
        CloseableHttpResponse httpResponse = null;
        int statusCode=0;
        try {
            HttpUriRequest httpUriRequest = getHttpUriRequest(request, site, headers);//获取Http请求对象，可以设置IP代理
            httpResponse = getHttpClient(site).execute(httpUriRequest); //执行请求操作
            Thread.sleep(site.getSleepTime());//休息下
            statusCode = httpResponse.getStatusLine().getStatusCode(); 
            request.putExtra(Request.STATUS_CODE, statusCode);
            if (statusAccept(acceptStatCode, statusCode)) {
                Page page = handleResponse(request, charset, httpResponse, task);
                return page;//返回页面
            } else {
                logger.warn("code error {} \t {}", statusCode, request.getUrl());
                return addToCycleRetry(request);
            }
        } /*catch (Exception e) {
        	
            logger.error("download page {} error {}", request.getUrl(), e);
            //FIXME 记录到Mongo
            if (site.getCycleRetryTimes() > 0) { //如果大于0，说明用户设置了重试次数，能执行到这里也说明重试次数没有超过。
            	logger.warn("download page {} try again, current retry times: {}", request.getUrl(),
            			(Integer) null == request.getExtra(Request.CYCLE_TRIED_TIMES) ? 1 : request.getExtra(Request.CYCLE_TRIED_TIMES));
                return addToCycleRetry(request);
            }else{//用户没有设置重试次数。直接return null，任务加入 Fail Collection。
            	return null;
            }
            
        }*/ finally {
        	request.putExtra(Request.STATUS_CODE, statusCode);
            try {
                if (httpResponse != null) {
                    EntityUtils.consume(httpResponse.getEntity());
                }
            } catch (IOException e) {
                logger.warn("close response fail", e);
            }
        }
    }

    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    /**
     * 判断响应状态码是否被接受
     * @param acceptStatCode
     * @param statusCode
     * @return
     */
    protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
        return acceptStatCode.contains(statusCode);
    }

    /**
     * 获取URI请求配置，包含代理设置、userAgent、cookie等
     * @param request
     * @param site 请求网站信息，包含请求cookie、失效时间等
     * @param headers 请求头信息
     * @return
     */
    public HttpUriRequest getHttpUriRequest(Request request, Site site, Map<String, String> headers) {
        RequestBuilder requestBuilder = selectRequestMethod(request).setUri(request.getUrl());
        if (MapUtils.isNotEmpty(headers)) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(site.getTimeOut())
                .setSocketTimeout(site.getTimeOut())
                .setConnectTimeout(site.getTimeOut())
                .setCookieSpec(CookieSpecs.BEST_MATCH)
                .setCircularRedirectsAllowed(true);//允许循环重定向！
        //设置代理proxy
        if (site.getHttpProxyPool() != null) { 
        	//随机，有时候有自己的IP
        	if (((int) (Math.random()*100))%((int)(100/hitRatio)) == 0) {
        		site.getHttpProxyPool().enable(false); //使用本地IP
        		logger.info("********** switch to user own ip.");
        	} else {
        		site.getHttpProxyPool().enable(true);
        	}
        	if (site.getHttpProxyPool().isEnable()) {
        		boolean isHttps = StringUtils.startsWith(request.getUrl(), "https");
        		HttpHost proxy = site.getHttpProxyFromPool(isHttps);
        		int i = 0;
                while (null == proxy) {
                	if ((i++) == 10) {
                		break;  //否则会进入死循环
                	}
                	ProxyPoolContext.getInstance().getProxyByAPI(isHttps); 
                	proxy = site.getHttpProxyPool().getProxy(isHttps);
                }
    			if (null != proxy) {
    				requestConfigBuilder.setProxy(proxy);
        			request.putExtra(Request.PROXY, proxy);
        			logger.info("user proxy >> ip:" + proxy.getAddress().getHostAddress() + " port:" + proxy.getPort());
    			}
        	}
		}else if(site.getHttpProxy()!= null){
            HttpHost host = site.getHttpProxy();
			requestConfigBuilder.setProxy(host);
			request.putExtra(Request.PROXY, host);	
		}
        
        requestBuilder.setConfig(requestConfigBuilder.build());
        return requestBuilder.build();
    }

    /**
     * 封装请求方法
     * @param request request.getMethod()
     * @return GET/POST/HEAD/PUT/DELETE/TRACE
     */
    private RequestBuilder selectRequestMethod(Request request) {
        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
            //default get
            return RequestBuilder.get();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
            RequestBuilder requestBuilder = RequestBuilder.post();
            NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
            if (nameValuePair != null && nameValuePair.length > 0) {
                requestBuilder.addParameters(nameValuePair);
            }
            return requestBuilder;
        } else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
            return RequestBuilder.head();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
            return RequestBuilder.put();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
            return RequestBuilder.delete();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
            return RequestBuilder.trace();
        }
        throw new IllegalArgumentException("Illegal HTTP Method " + method);
    }

    /**
     * 处理Http响应，将响应内容放在Page对象中
     * @param request
     * @param charset
     * @param httpResponse
     * @param task
     * @return
     * @throws IOException
     */
    private Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
        ContentAndCharset cc = getContentAndCharset(charset, httpResponse);
        Page page = new Page();
        page.setRawText(cc.getContent()); //内容体
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        page.putField("charset", cc.getCharset());
        return page;
    }

    /**
     * 查询响应内容
     * @param charset
     * @param httpResponse
     * @return
     * @throws IOException
     */
    protected String getContent(String charset, HttpResponse httpResponse) throws IOException {
        if (charset == null) {
            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
            String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
            if (htmlCharset != null) {
                return new String(contentBytes, htmlCharset);
            } else {
                logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()", Charset.defaultCharset());
                return new String(contentBytes);
            }
        } else {
            return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
        }
    }
    
    /**
     * 查询响应内容
     * @param charset
     * @param httpResponse
     * @return
     * @throws IOException
     */
    private ContentAndCharset getContentAndCharset(String charset, HttpResponse httpResponse) throws IOException {
    	ContentAndCharset cc = new ContentAndCharset();
        if (charset == null) {//没有指定编码
 //       	HttpEntity entity = httpResponse.getEntity();
//        	if(entity instanceof GzipDecompressingEntity){
//				GzipDecompressingEntity gdEntity = (GzipDecompressingEntity)entity;
//				InputStream content = gdEntity.getContent();
//				int i = content.available();
//				Reader reader = new InputStreamReader(content);
//				CharArrayBuffer buffer = new CharArrayBuffer(i);
//				final char[] tmp = new char[1024];
//	            int l;
//	            try{
//	            	while((l = reader.read(tmp)) != -1) {
//	            		buffer.append(tmp, 0, l);
//	            	}
//	            } catch(EOFException e) {
//	            	logger.error("EOF,{}",e);
//	            } finally {
//	            	reader.close();
//	            }
//	             cc.setContent(new String(buffer.toCharArray()));
//        	}else{//此处可能报EOFException。上面的代码能解决问题，但有乱码问题。TODO 后续如果遇到会报EOFException的网站再处理
	            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
	            String htmlCharset = getHtmlCharset(httpResponse, contentBytes);//自动获取编码
	            cc.setCharset(htmlCharset);
	            if (htmlCharset != null) {
	                cc.setContent(new String(contentBytes, htmlCharset));
	            } else {
	            	cc.setContent(new String(contentBytes, "utf-8"));
	                logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()", Charset.defaultCharset());
	            }
        //	}
        } else {//指定了编码
            cc.setContent(IOUtils.toString(httpResponse.getEntity().getContent(), charset));;
            cc.setCharset(charset);
        }
        return cc;
    }
    
    class ContentAndCharset {
    	private String content;
    	private String charset;
    	public ContentAndCharset() {}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public String getCharset() {
			return charset;
		}
		public void setCharset(String charset) {
			this.charset = charset;
		}
    }

    /**
     * 获取HTML的编码Charset
     * @param httpResponse
     * @param contentBytes
     * @return
     * @throws IOException
     */
    public String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        String charset = null;
        // charset
        // 1、encoding in http header Content-Type
        String value = httpResponse.getEntity().getContentType().getValue();
        charset = UrlUtils.getCharset(value);
        if (StringUtils.isNotBlank(charset)) {
            logger.debug("Auto get charset: {}", charset);
            return charset;
        }
        // use default charset to decode first time
        Charset defaultCharset = Charset.defaultCharset();
        String content = new String(contentBytes, defaultCharset.name());
        // 2、charset in meta
        if (StringUtils.isNotEmpty(content)) {
            Document document = Jsoup.parse(content);
            Elements links = document.select("meta");
            for (Element link : links) {
                // 2.1、html4.01 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                String metaContent = link.attr("content");
                String metaCharset = link.attr("charset");
                if (metaContent.indexOf("charset") != -1) {
                    metaContent = metaContent.substring(metaContent.indexOf("charset"), metaContent.length());
                    charset = metaContent.split("=")[1];
                    break;
                }
                // 2.2、html5 <meta charset="UTF-8" />
                else if (StringUtils.isNotEmpty(metaCharset)) {
                    charset = metaCharset;
                    break;
                }
            }
        }
        logger.debug("Auto get charset: {}", charset);
        // 3、todo use tools as cpdetector for content decode
        return charset;
    }
    
    public HttpClientDownloader setUserAgentContext(UserAgentContext userAgent) {
    	this.userAgent = userAgent;
    	return this;
    }
    
    public String getUserAgentStates() {
    	if (userAgent != null) {
    		return this.userAgent.getAllUsedState();
    	} else {
    		return "";
    	}
    }

	@Override
	public void close() throws IOException {
    	logger.info("user-agent used stated:");
    	logger.info("\n" + getUserAgentStates());
	}
    
}
