package com.hlg.webgleaner.core.rmimonitor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * quartz任务管理RMI组件，方便在web界面上进行任务添加，任务时间修改，任务删除等操作。
 * 持有一个SchedulerFactoryBean类对象，需要在服务端进行实例化。
 * 
 * @author yangwq
 * @Date 2016年5月25日
 */
public class QuartzMonitorRMIImpl extends UnicastRemoteObject implements QuartzMonitorRMI {

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private SchedulerFactoryBean schedulerFactory;

	public QuartzMonitorRMIImpl(SchedulerFactoryBean factory) throws RemoteException {
		super();
		this.schedulerFactory = factory;
	}

	public List<Trigger> showJobDetails() throws SchedulerException {
		Scheduler scheduler = schedulerFactory.getScheduler();
		Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
		List<Trigger> triggers = new ArrayList<Trigger>();
		for (JobKey jobKey : jobKeys) {
			List<? extends Trigger> triggersOfJob = scheduler.getTriggersOfJob(jobKey);
			triggers.addAll(triggersOfJob);
		}
		return triggers;
	}

	@SuppressWarnings("unchecked")
	public void addJob(String jobName, String jobGroupName, String triggerName, String triggerGroupName,
			String className, String time) {
		Scheduler scheduler = schedulerFactory.getScheduler();
		JobDetailImpl jd = new JobDetailImpl();
		jd.setName(jobName);
		jd.setGroup(jobGroupName);
		Class<? extends Job> clazz = null;
		try {
			clazz = (Class<? extends Job>) Class.forName(className);
		} catch (ClassNotFoundException e2) {

		}
		jd.setJobClass(clazz);
		CronTriggerImpl trigger = new CronTriggerImpl();
		trigger.setJobName(jobName);
		JobKey jobKey = new JobKey(jobName, jobGroupName);
		trigger.setJobKey(jobKey);
		trigger.setName(triggerName);
		trigger.setGroup(triggerGroupName);
		try {
			trigger.setCronExpression(time);
		} catch (ParseException e) {
			log.error("时间表达式解析异常:{}", e);
		}
		try {
			scheduler.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			log.error("quartz调度异常:{}", e);
		}

	}

	@Override
	public void modifyJobTime(String triggerName, String triggerGroupName, String cronExpression)
			throws SchedulerException, ParseException {
		Scheduler scheduler = schedulerFactory.getScheduler();
		TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroupName);
		Trigger trigger = scheduler.getTrigger(triggerKey);
		
		if (trigger != null) {
			CronTriggerImpl ct = (CronTriggerImpl) trigger;
			//ct.setDescription(description);
			ct.setCronExpression(cronExpression);
			scheduler.rescheduleJob(triggerKey, ct);
		}
	}

	@Override
	public void deleteJob(String name, String group) {
		Scheduler scheduler = schedulerFactory.getScheduler();
		TriggerKey triggerKey = TriggerKey.triggerKey(name, group);
		Trigger trigger = null;
		try {
			trigger = scheduler.getTrigger(triggerKey);
			JobKey jobKey = trigger.getJobKey();
			scheduler.deleteJob(jobKey);
		} catch (SchedulerException e) {
			log.error("quartz调度器异常:{}", e);
		}
	}

	@Override
	public void setDescription(String triggerName, String triggerGroupName, String description)
			throws RemoteException, SchedulerException {
		Scheduler scheduler = schedulerFactory.getScheduler();
		TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroupName);
		Trigger trigger = scheduler.getTrigger(triggerKey);
		if (trigger != null) {
			CronTriggerImpl ct = (CronTriggerImpl) trigger;
			ct.setDescription(description);
			scheduler.rescheduleJob(triggerKey, ct);
		}
	}

}
