package com.hlg.webgleaner.core.rmimonitor;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Swap;

public interface HostParamMonitorRMI extends Remote {

	/**
	 * CPU信息
	 * @return
	 */
	CpuPerc getCpu() throws RemoteException;

	/**
	 * 内存信息
	 * @return
	 */
	Mem getMem() throws RemoteException;

	/**
	 * 操作系统信息
	 */
	OperatingSystem getHostParam() throws RemoteException;

	/**
	 * 交换区信息
	 */
	Swap getSwap() throws RemoteException;

}