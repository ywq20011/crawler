package com.hlg.webgleaner.core.rmimonitor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 宿主机器参数RMI组件。获取cpu，内存，机器基础信息。
 * 使用Sigar组件。
 * 
 * @author yangwq
 * @Date 2016年6月1日
 */
public class HostParamMonitorRMIImpl extends UnicastRemoteObject implements HostParamMonitorRMI {

	public HostParamMonitorRMIImpl() throws RemoteException {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Sigar sigar = new Sigar();
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * CPU信息
	 * @return
	 */
	@Override
	public CpuPerc getCpu(){
		CpuPerc cpuPerc = null;
		try {
			cpuPerc = sigar.getCpuPerc();
		} catch (SigarException e) {
			log.error("获取cpu信息失败:{}",e);
		}
		return cpuPerc;
	}
	
	/**
	 * 内存信息
	 * @return
	 */
	@Override
	public Mem getMem(){
		
		Mem men = null;
		try {
			men = sigar.getMem();
		} catch (SigarException e) {
			log.error("获取内存信息失败:{}",e);
		}
		 
		return men;
	}
	
	/**
	 * 交换区信息
	 * @return
	 */
	@Override
	public Swap getSwap(){
		Swap swap = null;
		try {
			swap = sigar.getSwap();
		} catch (SigarException e) {
			log.error("获取交换区信息失败:{}",e);
		}
		return swap;
		
	}
	
	/**
	 * 操作系统信息
	 */
	@Override
	public OperatingSystem getHostParam(){
		OperatingSystem os = OperatingSystem.getInstance();
		return os;
	}
	
}
