package us.codecraft.webmagic.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import us.codecraft.webmagic.Spider;
/**
 * RMI操作Quartz任务调度的爬虫。提供手动控制和自动纠错控制两种方式。
 * 
 * @author yangwq
 * @Date 2016年6月6日
 */
public interface QuartzSpiderRMI extends Remote {
	
	/**
	 * 获取map中爬虫的数量
	 * @return
	 * @throws RemoteException
	 */
	int getSpiderNum() throws RemoteException;

	/**
	 * 销毁爬虫
	 * @param key
	 * @throws RemoteException
	 */
	void removeSpider(String key) throws RemoteException;
}