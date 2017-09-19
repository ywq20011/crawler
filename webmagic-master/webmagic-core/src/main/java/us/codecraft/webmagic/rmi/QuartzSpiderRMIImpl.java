package us.codecraft.webmagic.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import us.codecraft.webmagic.Spider;

/**
 * Quartz框架调度的爬虫实例RMI服务类。
 * 采用单例模式，一台机器拥有一个单实例，允许服务端的其他代码（corrector组件）获取到该单例。
 * 它拥有一个存放Quartz框架调度时实例化的Spider对象的map数据结构，并提供4个客户端接口：
 * 1.removeSpider(String) 允许客户端通过爬虫monitorId停止并移除map中的爬虫实例；
 * 2.getSpiderNum() 允许客户端获取当前机器下运行的quartz爬虫个数；
 * 所以当前版本并没有给客户端暴露全部接口，如果后续有需要可以添加，如果不需要甚至可以取消远程接口，完全由Corrector组件进行自动调用。
 * 这个类中的方法主要为Corrector组件所调用。
 * 
 * @author yangwq
 * @Date 2016年6月1日
 */
public class QuartzSpiderRMIImpl extends UnicastRemoteObject implements QuartzSpiderRMI  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static ConcurrentMap<Long,Spider> autoSpiderMap = new ConcurrentHashMap<Long,Spider>();
	
	private QuartzSpiderRMIImpl() throws RemoteException {
		super();
	}

	private static QuartzSpiderRMIImpl quartzSpiderRMIImpl;
	
	public static QuartzSpiderRMIImpl getInstance() throws RemoteException{
		if(quartzSpiderRMIImpl == null){
			quartzSpiderRMIImpl = new QuartzSpiderRMIImpl();
		}
		return quartzSpiderRMIImpl;
	}
	
	/**
	 * 放入爬虫
	 * @param key
	 * @param spider
	 */
	public void putSpider(Long key,Spider spider)  {
		autoSpiderMap.put(key, spider);
	}
	
	/**
	 * 复活爬虫
	 * @param key
	 */
	public void reviveSpider(String key) {
		Spider spider = autoSpiderMap.get(key);
		if(spider!=null){
			spider.runAsync();
		}
	}

	/**
	 * 获取爬虫的map
	 */
	public ConcurrentMap<Long, Spider> getSpiderMap() {
		return autoSpiderMap;
	}
	
	/**
	 * 销毁爬虫
	 */
	@Override
	public void removeSpider(String key) throws RemoteException{
		Spider spider = autoSpiderMap.get(key);
		if(spider!=null){
			spider.setStat(new AtomicInteger(2));
			autoSpiderMap.remove(key);
		}
	}
	
	/**
	 * 获取当前Map中的爬虫个数
	 */
	@Override
	public int getSpiderNum() throws RemoteException{
		return autoSpiderMap.size();
	}

	/**
	 * 获取自动化爬虫的map
	 * @return
	 */
	public static Map<Long, Spider> getAutoSpiderMap() {
		if(autoSpiderMap == null){
			return null;
		}
		return autoSpiderMap;
	}
}

