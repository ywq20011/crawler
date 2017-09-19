package us.codecraft.webmagic.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.utils.FilePersistentBase;
import us.codecraft.webmagic.utils.ProxyUtils;

/**
 * http代理池
 * Pooled Proxy Object
 * @author yxssfxwzy@sina.com <br>
 * @see Proxy
 * @update linjx@linesum.com
 * 修改获取策略和
 * @since 0.5.1
 */
public class ProxyPool {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private DelayQueue<Proxy> httpProxyQueue = new DelayQueue<Proxy>();
    private DelayQueue<Proxy> httpsProxyQueue = new DelayQueue<Proxy>();
    private Map<String, Proxy> allProxy = new ConcurrentHashMap<String, Proxy>();
    
    private int reuseInterval = 4000;// ms，这个逻辑由DelayQueue实现
    private int reviveTime = 2 * 60 * 60 * 1000;// ms
    private int saveProxyInterval = 10 * 60 * 1000;// ms

    private boolean isEnable = false;
    private boolean validateWhenInit = false;
    // private boolean isUseLastProxy = true;
    private String proxyFilePath = "/data/webmagic/lastUse.proxy";
    
    private boolean isUseVPS = false;

    private FilePersistentBase fBase = new FilePersistentBase();

    private Timer timer = new Timer(true);
    private TimerTask saveProxyTask = new TimerTask() {
        @Override
        public void run() {
            saveProxyList();
            logger.info(allProxyStatus());
        }
    };

    public ProxyPool() {
        this(null, false);
    }

    public ProxyPool(List<ProxyIP> httpProxyList) {
        this(httpProxyList, false);
    }
    
    public ProxyPool(List<ProxyIP> httpProxyList, boolean isPersistentProxy) {
        this(httpProxyList, isPersistentProxy, null);
    }

    public ProxyPool(List<ProxyIP> httpProxyList, boolean isPersistentProxy, String proxyFilePath) {
        if (CollectionUtils.isNotEmpty(httpProxyList)) {
            addProxy(httpProxyList.toArray(new ProxyIP[]{}));
        }
        if (StringUtils.isNotBlank(proxyFilePath)) {
        	this.proxyFilePath = proxyFilePath;
        }
        if (isPersistentProxy) {
            File file = new File(this.proxyFilePath);
            if (!file.exists()) {
    			if (!file.getParentFile().exists()) {
    				file.mkdirs();
    			}
    			try {
					file.createNewFile();
				} catch (IOException e) {
					logger.error("read proxy file error:", e);
				}
    		}
            readProxyList();
            timer.schedule(saveProxyTask, 0, saveProxyInterval);
        }
    }

    /**
     * 一次性存储所有代理
     */
    public void saveProxyList() {
        if (allProxy.size() == 0) {
            return;
        }
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fBase.getFile(proxyFilePath)));
            os.writeObject(prepareForSaving());
            os.close();
            logger.info("save proxy");
        } catch (FileNotFoundException e) {
            logger.error("proxy file not found", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Proxy> prepareForSaving() {
        Map<String, Proxy> tmp = new HashMap<String, Proxy>();
        for (Entry<String, Proxy> e : allProxy.entrySet()) {
            Proxy p = e.getValue();
            p.setFailedNum(p.getFailedNum());  //不合理
            tmp.put(e.getKey(), p);
        }
        return tmp;
    }

    /**
     * 从文件中一次性加载所有代理IP 
     */
    @SuppressWarnings("unchecked")
	private void readProxyList() {
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(fBase.getFile(proxyFilePath)));
            addProxy((Map<String, Proxy>) is.readObject());
            is.close();
        } catch (FileNotFoundException e) {
            logger.info("last use proxy file not found", e);
        } catch (IOException e) {
        	logger.info("last use proxy file IOException", e);
        } catch (ClassNotFoundException e) {
        	logger.info("last use proxy mapped class not found:", e);
        }
    }
    
    private void addProxy(Map<String, Proxy> httpProxyMap) {
        isEnable = true;
        for (Entry<String, Proxy> entry : httpProxyMap.entrySet()) {
            try {
                if (allProxy.containsKey(entry.getKey())) {
                    continue;
                }
                if (!validateWhenInit || ProxyUtils.validateProxy(entry.getValue().getHttpHost())) {
                    entry.getValue().setFailedNum(0);
                    entry.getValue().setReuseTimeInterval(reuseInterval);
                    if (entry.getValue().isHttps()) {
                    	httpsProxyQueue.add(entry.getValue());
                    } else {
                    	httpProxyQueue.add(entry.getValue());
                    }
                    allProxy.put(entry.getKey(), entry.getValue());
                }
            } catch (NumberFormatException e) {
                logger.error("HttpHost init error:", e);
            }
        }
        logger.info("proxy pool size>>>>" + allProxy.size());
    }

    /**
     * s[0]--ip，s[1]--port，默认是http
     * @param httpProxyList
     */
    public void addProxy(String[]... httpProxyList) {
        isEnable = true;
        for (String[] s : httpProxyList) {
            try {
                if (allProxy.containsKey(s[0])) {
                    continue;
                }
                HttpHost item = new HttpHost(InetAddress.getByName(s[0]), Integer.valueOf(s[1]));
                if (!validateWhenInit || ProxyUtils.validateProxy(item)) {
                    Proxy p = new Proxy(item, reuseInterval);
                    if (p.isHttps()) {
                    	httpsProxyQueue.add(p);
                    } else {
                    	httpProxyQueue.add(p);
                    }
                    allProxy.put(s[0], p);
                }
            } catch (NumberFormatException e) {
                logger.error("HttpHost init error:", e);
            } catch (UnknownHostException e) {
                logger.error("HttpHost init error:", e);
            }
        }
        logger.info("proxy pool size>>>>" + allProxy.size());
    }
    
    /**
     * 新增Proxy
     * @param httpProxyList
     */
    public void addProxy(ProxyIP... httpProxyList) {
        isEnable = true;
        int i = 0;
        for (ProxyIP p : httpProxyList) {
            if (allProxy.containsKey(p.getIp())) {
                continue;
            }
            try {
	            HttpHost item = new HttpHost(InetAddress.getByName(p.getIp()), p.getPort());
	            if (!validateWhenInit || ProxyUtils.validateProxy(item)) {
	                Proxy hp = new Proxy(item, reuseInterval);
	                hp.setHttps(p.getIsHttps());
	                if (hp.isHttps()) {
	                	httpsProxyQueue.add(hp);
                    } else {
                    	httpProxyQueue.add(hp);
	                }
	                allProxy.put(p.getIp(), hp);
	                i ++;
	            }
            } catch (UnknownHostException e) {
				logger.error("HttpHost init error:", e);
			}
        }
        logger.info("proxy pool idlenum>>>>" + getIdleNum());
        logger.info("new added proxy num>>>>" + i);
    }
    
    /**
     * 新增Proxy
     * @param httpProxyList
     */
    public void addVpsProxy(int reuseInterval, ProxyIP... httpProxyList) {
        isEnable = true;
        isUseVPS = true;
        int i = 0;
        for (ProxyIP p : httpProxyList) {
            if (allProxy.containsKey(p.getIp())) {
                continue;
            }
            try {
            	HttpHost item = new HttpHost(InetAddress.getByName(p.getIp()), p.getPort());
            	Proxy hp = new Proxy(item, reuseInterval);
            	hp.setHttps(p.getIsHttps());
            	if (hp.isHttps()) {
            		httpsProxyQueue.add(hp);
            	} else {
            		httpProxyQueue.add(hp);
            	}
            	allProxy.put(p.getIp(), hp);
            	i ++;
            } catch (UnknownHostException e) {
				logger.error("HttpHost init error:", e);
			}
        }
        logger.info("proxy pool idlenum>>>>" + getIdleNum());
        logger.info("new added vps proxy num>>>>" + i);
    }

	public HttpHost getProxy(boolean isHttps) {
        Proxy proxy = null;
        Long time = System.currentTimeMillis();
        try {
        	 if (isUseVPS) {
        		 if (isHttps) {
                  	proxy = httpsProxyQueue.take();
                  } else {
                  	proxy = httpProxyQueue.take();
                  }
        	 } else {
        		 if (isHttps) {
                  	proxy = httpsProxyQueue.poll(5000, TimeUnit.MILLISECONDS);
                  } else {
                  	proxy = httpProxyQueue.poll(5000, TimeUnit.MILLISECONDS);
                  }
        	}
		} catch (Exception e) {
			logger.error("take proxy error:", e);
		}
        double costTime = (System.currentTimeMillis() - time) / 1000.0;
        if (proxy == null) {
        	logger.warn("get proxy none and cost time " + costTime);
        	try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	return null;
        }
        logger.info("get proxy time >>>> " + costTime + " >>> " + proxy.getHttpHost());
        Proxy p = allProxy.get(proxy.getHttpHost().getAddress().getHostAddress());
        p.setLastBorrowTime(System.currentTimeMillis());
        p.borrowNumIncrement(1);
        
        return proxy.getHttpHost();
    }

    /**
     * 返回结果，修改剔除策略
     * @param host
     * @param statusCode
     */
    public void returnProxy(HttpHost host, int statusCode, String requestUrl) {
    	if (null == host) {
    		return;
    	}
        Proxy p = allProxy.get(host.getAddress().getHostAddress());
        if (p == null) {
            return;
        }
        if (p.isVps()) {
        	//vps
        	switch (statusCode) { //只有成功的才继续使用
	            case Proxy.SUCCESS:
	            	p.setReuseTimeInterval(p.getReuseTimeInterval());
	                p.setFailedNum(0);
	                p.setFailedErrorType(new ArrayList<Integer>());
	                p.recordResponse();
	                p.successNumIncrement(1);
	                if (p.isHttps()) {
	                	httpsProxyQueue.put(p);
	                } else {
	                	httpProxyQueue.put(p);
	                }
	                logger.info("success vps proxy:" + p.getHttpHost() + " >>>> request:" + requestUrl);
	            	break;
	            default:
	            	if (p.isHttps()) {
	                	httpsProxyQueue.put(p);
	                } else {
	                	httpProxyQueue.put(p);
	                }
	            	logger.info("error vps proxy:" + p.getHttpHost() + " >>>> request:" + requestUrl);
	            	break;
        	}
        } else { //非vps
        	switch (statusCode) { //只有成功的才继续使用
	            case Proxy.SUCCESS:
	                p.setReuseTimeInterval(reuseInterval);
	                p.setFailedNum(0);
	                p.setFailedErrorType(new ArrayList<Integer>());
	                p.recordResponse();
	                p.successNumIncrement(1);
	                if (p.isHttps()) {
	                	httpsProxyQueue.put(p);
	                } else {
	                	httpProxyQueue.put(p);
	                }
	                logger.info("success proxy:" + p.getHttpHost() + " >>>> request:" + requestUrl);
	                break;
	            case Proxy.ERROR_403:
	                // banned,try longer interval
	                p.fail(Proxy.ERROR_403);
	                p.setReuseTimeInterval(reuseInterval * p.getFailedNum());
	                p.setReuseTimeInterval(reuseInterval * p.getFailedNum());
	                allProxy.remove(host.getAddress().getHostAddress()); //直接丢弃，不重入queue就不会被反复调用，最后输出allProxy的状态信息，可以用于统计
	                logger.info(host + " >>>> reuseTimeInterval is >>>> " + p.getReuseTimeInterval() / 1000.0);
	                break;
	            case Proxy.ERROR_BANNED:
	                p.fail(Proxy.ERROR_BANNED);
	                p.setReuseTimeInterval(10 * 60 * 1000 * p.getFailedNum());
	                allProxy.remove(host.getAddress().getHostAddress()); //直接丢弃，不重入queue就不会被反复调用，最后输出allProxy的状态信息，可以用于统计
	                logger.warn("this proxy is banned >>>> " + p.getHttpHost());
	                logger.info(host + " >>>> removed >>>> ");
	                break;
	            case Proxy.ERROR_404:
	                 p.fail(Proxy.ERROR_404);
	                 p.setReuseTimeInterval(reuseInterval * p.getFailedNum());
	                 allProxy.remove(host.getAddress().getHostAddress()); //直接丢弃，不重入queue就不会被反复调用，最后输出allProxy的状态信息，可以用于统计
	                 logger.warn("this proxy is error: " + Proxy.ERROR_404);
	                break;
	            default:
	                p.fail(statusCode);
	                p.setReuseTimeInterval(reuseInterval * p.getFailedNum());
	                allProxy.remove(host.getAddress().getHostAddress()); //直接丢弃，不重入queue就不会被反复调用，最后输出allProxy的状态信息，可以用于统计
	                break;
        	}
        	if (p.getFailedNum() > 2) {
        		allProxy.remove(p.getHttpHost());  //也直接在map中移除，一边快速获取map，防止map太大
        		logger.warn("remove proxy >>>> " + host + ">>>>" + p.getFailedType() + " >>>> remain proxy >>>> " + allProxy.size());
        		return;
        	}
        }
        
    }

    public String allProxyStatus() {
        String re = "all proxy info >>>> \n";
        for (Entry<String, Proxy> entry : allProxy.entrySet()) {
            re += entry.getValue().toString() + "\n";
        }
        return re;
    }

    public int getIdleNum() {
        return httpProxyQueue.size() + httpsProxyQueue.size(); //这个时间不是实时的
    }
    
    public int getHttpIdleNum() {
    	return httpProxyQueue.size();
    }
    
    public int getHttpsIdleNum() {
    	return httpsProxyQueue.size();
    }
    
    public boolean isEmptyHttpsProxy() {
    	return httpsProxyQueue.isEmpty();
    }

    public int getReuseInterval() {
        return reuseInterval;
    }

    public void setReuseInterval(int reuseInterval) {
        this.reuseInterval = reuseInterval;
    }

    public ProxyPool enable(boolean isEnable) {
        this.isEnable = isEnable;
        return this;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public int getReviveTime() {
        return reviveTime;
    }

    public void setReviveTime(int reviveTime) {
        this.reviveTime = reviveTime;
    }

    public boolean isValidateWhenInit() {
        return validateWhenInit;
    }

    public void validateWhenInit(boolean validateWhenInit) {
        this.validateWhenInit = validateWhenInit;
    }

    public int getSaveProxyInterval() {
        return saveProxyInterval;
    }

    public void setSaveProxyInterval(int saveProxyInterval) {
        this.saveProxyInterval = saveProxyInterval;
    }

    public String getProxyFilePath() {
        return proxyFilePath;
    }

    public void setProxyFilePath(String proxyFilePath) {
        this.proxyFilePath = proxyFilePath;
    }

	public void setUseVPS(boolean isUseVPS) {
		this.isUseVPS = isUseVPS;
	}

}
