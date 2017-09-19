package com.hlg.webgleaner.core.listener;

import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * Corrector远程接口。只暴露了获取日志的接口方法。
 * 
 * @author yangwq
 * @Date 2016年6月8日
 */
public interface WebgleanerMonitorCorrectorRemote extends Remote {

	/**
	 * 获取自动调度爬虫的最近20条日志
	 * @return
	 * @throws RemoteException
	 */
	LimitQueue<String> getAutoLogList() throws RemoteException;
	
	/**
	 * 获取手动调度爬虫的最近20条日志
	 * @return
	 * @throws RemoteException
	 */
	LimitQueue<String> getManualLogList() throws RemoteException;

	
}
