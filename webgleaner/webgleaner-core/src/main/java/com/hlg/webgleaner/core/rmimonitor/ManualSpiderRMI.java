package com.hlg.webgleaner.core.rmimonitor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
/**
 * 爬虫RMI远程接口。显示爬虫工厂所有方法，调用方法，以及停止爬虫方法。
 * @author yangwq
 * @Date 2016年5月20
 */
public interface ManualSpiderRMI extends Remote {

	/**
	 * 获取爬虫工厂所有方法
	 * @return
	 * @throws RemoteException
	 */
	Map<String,List<String>>  showSpiderMethods() throws RemoteException;
	
	/**
	 * 调用爬虫工厂方法，包括爬虫方法和非爬虫方法。
	 * @param methodName
	 * @throws RemoteException
	 */
	void invoke(String methodName) throws RemoteException;

	/**
	 * 停止爬虫方法。
	 * @param methodName
	 * @throws RemoteException
	 */
	void removeSpider(String methodName) throws RemoteException;

	/**
	 * 获取所有绑定对象的简单类名。
	 * @return
	 * @throws RemoteException
	 */
	List<String> getSimpleClassNames() throws RemoteException;
	
}
