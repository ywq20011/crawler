package com.hlg.webgleaner.core.rmimonitor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.List;

import org.quartz.SchedulerException;
import org.quartz.Trigger;

public interface QuartzMonitorRMI extends Remote {

	/**
	 * 显示计划中的任务信息，包括任务名称，任务group名称，任务触发下一次时间，任务上一次触发时间等。
	 * @return 返回Trigger列表
	 * @throws SchedulerException
	 * @throws RemoteException
	 */
	List<Trigger> showJobDetails() throws SchedulerException,RemoteException;
	
	/**
	 * 添加一个任务。参数jobName和jobGroupName用于生成JobDetail实例。
	 * 参数triggerName、triggerGroupName、time用于生成Trigger实例。
	 * 生成的jobDetail和Trigger将被scheduler绑定。
	 * @param jobName 任务名称
	 * @param jobGroupName 任务群名称
	 * @param triggerName 触发器名称
	 * @param triggerGroupName 触发器群名称
	 * @param clazz 任务类全类名
	 * @param time cronExpression
	 * @throws RemoteException 
	 */
	void addJob(String jobName,String jobGroupName,String triggerName,String triggerGroupName ,String clazz,String time) throws RemoteException;
	
	/**
	 * 修改触发器触发时间，结果是和该触发器绑定的任务的触发时间得到修改。
	 * @param triggerName
	 * @param triggerGroupName
	 * @param cronExpression
	 * @throws SchedulerException
	 * @throws ParseException
	 * @throws RemoteException
	 */
    void modifyJobTime(String triggerName,String triggerGroupName,String cronExpression) throws SchedulerException, ParseException, RemoteException;
   
	/**
	 * 删除任务。
	 * @param name
	 * @param group
	 * @throws RemoteException
	 */
	void deleteJob(String name, String group) throws RemoteException;

	/**
	 * 添加描述。
	 * @param triggerName
	 * @param triggerGroupName
	 * @param cronExpression
	 * @throws RemoteException
	 */
	void setDescription(String triggerName,String triggerGroupName,String description) throws RemoteException,SchedulerException;
	
}
