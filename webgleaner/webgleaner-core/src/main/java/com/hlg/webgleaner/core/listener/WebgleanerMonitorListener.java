package com.hlg.webgleaner.core.listener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.hlg.webgleaner.core.listener.bo.ErrorMessage;
import com.hlg.webgleaner.core.listener.bo.FailedUrlBo;
import com.hlg.webgleaner.core.listener.bo.Monitor;
import com.hlg.webgleaner.core.listener.bo.MonitorMessage;
import com.hlg.webgleaner.core.listener.bo.Speed;
import com.mongodb.DBCollection;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.utils.IdUtils;

/**
 * 爬虫监听器.记录爬虫运行状态。
 * 
 * @author yangwq
 * @date 2016年5月4日
 */
public class WebgleanerMonitorListener implements SpiderListener {

	/**
	 * corrector组件单例
	 */
	private WebgleanerMonitorCorrector corrector;
	private Logger log = LoggerFactory.getLogger(WebgleanerMonitorListener.class);
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private MongoTemplate template ;
	
	/**
	 * Monitor对象，储存爬虫动态运行状况数据。
	 */
	private Monitor monitor = new Monitor();
	
	/**
	 * 每成功250次时，进行一次监控数据持久化。
	 */
	private int successThreadhold = 250;
	
	/**
	 * 构造器，初始化监控数据，初始化corrector单例等
	 */
	public WebgleanerMonitorListener(MongoTemplate template,String collectionName) {
		this.template = template;
		corrector = WebgleanerMonitorCorrector.getInstance();
		InetAddress addr = null;
		try {
			addr = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.error("无法获取主机IP地址:{}", e);
		}
		monitor.setCollectionName(collectionName);
		
		String[] ips = addr.getHostAddress().toString().split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String str : ips) {
			sb.append(str);
		}
		monitor.setIp(sb.toString());
		monitor.setMonitorCollectionName("monitor");
		monitor.setErrorCollectionName("error" + monitor.getIp() + sdf.format(new Date()));
	}
	
	/**
	 * 可以设置成功多少次数时进行持久化的构造器
	 */
	public WebgleanerMonitorListener(MongoTemplate template,String collectionName,int successThreadHold){
		this(template,collectionName);
		this.successThreadhold = successThreadHold;
	}
	
	/**
	 * 访问listener的monitro实例。便于corrector获得对应listener的monitor，进行纠错检测。
	 * @return
	 */
	public Monitor getMonitor() {
		return monitor;
	}

	/**
	 * listener初始化时调用，记录状态，启动时间，记录quartz爬虫远程绑定地址。
	 */
	@Override
	public void onStart(Date date,boolean regist ,String searchId,Long monitorId) {
		monitor.setMonitorId(monitorId);
		monitor.setState("running");
		monitor.setStartTime(date);
		if(regist){//如果放入了map中，说明是quartz任务调度的爬虫，monitor中设置RMI地址。
			try {
				monitor.setRemoteAddress("rmi://"+InetAddress.getLocalHost().getHostAddress()+":8888/quartzSpiders");
			} catch (UnknownHostException e) {
				log.error("找不到主机地址:{}",e);
			}
		}
		log.info("爬虫监控启动...");
	}

	/**
	 * 爬取成功时调用，记录成功次数，各组件耗时。每成功一定次数时进行持久化。
	 */
	@Override
	public synchronized void onSuccess(Request request) {
//		if (monitor.getCollectionName() == null) {
//			monitor.setCollectionName((String) request.getExtra("coll"));
//		}
//		if (monitor.getSpiderType() == null) {
//			monitor.setSpiderType(determineSpiderType(monitor.getCollectionName()));
//		}
		monitor.getSuccessCount().incrementAndGet();// 成功任务次数加1
		monitor.setEndTime(null);
		// 增加downloader耗时
		Long downloaderTime = (Long) request.getExtra("downloaderTime");
		monitor.setDownloaderSpend(monitor.getDownloaderSpend() + downloaderTime);
		// 增加pageProcessor耗时
		Long pageProcessorTime = (Long) request.getExtra("pageProcessorTime");
		if(pageProcessorTime !=null ){
			monitor.setPageProcessorSpend(monitor.getPageProcessorSpend() + pageProcessorTime);
		}
		// 增加pipeline耗时
		Long pipelineTime = (Long) request.getExtra("pipelineTime");
		monitor.setPipelineSpend(monitor.getPipelineSpend() + pipelineTime);
		if (monitor.getSuccessCount().get() % successThreadhold == 0) {// 成功250次记录一次
			snapShotBeforePersist(request);
			persistMonitorMessage();
		}
	}

	/**
	 * 抛异常时调用。记录失败次数，异常信息。
	 */
	@Override
	public void onError(Request request) {
//		if (monitor.getCollectionName() == null)
//			monitor.setCollectionName((String) request.getExtra("coll"));
//		if (monitor.getSpiderType() == null) {
//			monitor.setSpiderType(determineSpiderType(monitor.getCollectionName()));
//		}
		monitor.getErrorCount().incrementAndGet();// 失败次数加1
		// 添加错误信息
		ErrorMessage em = new ErrorMessage();
		em.setId(IdUtils.id());
		em.setErrorTime(new Date());
		em.setErrorUrl(request.getUrl());
		em.setExceptionMessage((String) request.getExtra("exceptionMessage"));
		monitor.getErrorMessages().add(em);
	}

	/**
	 * 任务超过重试次数时调用。持久化废弃任务。
	 */
	@Override
	public void onFail(Request request) {
		FailedUrlBo fub = new FailedUrlBo();
		fub.setUrl(request.getUrl());
		fub.setRetryTimes((Integer) request.getExtra(Request.CYCLE_TRIED_TIMES));
		template.insert(fub,"failedURL" + sdf.format(new Date()));
	}

	/**
	 * 爬虫结束时调用。设置爬虫状态，设置结束时间。做最后一次持久化。把自己从map中移除。
	 */
	@Override
	public synchronized void onClose( ) {
		monitor.setState("stopped");
		monitor.setEndTime(new Date());// 记录结束时间
		snapShotBeforePersistLast();
		persistMonitorMessageLast();// 关闭前执行一次持久化
		Map<Long, Spider> autoSpiderMap = corrector.getAutoSpiderMap();
		//去除任务结束的auto spider,如果有的话
		autoSpiderMap.remove(monitor.getMonitorId());
		Map<Long, Spider> manualSpiderMap = corrector.getManualSpiderMap();
		//去除任务结束的manual spider,如果有的话
		if(manualSpiderMap != null){
			manualSpiderMap.remove(monitor.getMonitorId());
		}
	}

	/**
	 * 持久化前调用一次。更新剩余任务数，活跃线程数,uuid。首次记录时更改字段为first为false。
	 * 
	 * @param request
	 */
	private void snapShotBeforePersist(Request request) {
		DBCollection coll = template.getCollection(monitor.getCollectionName());
		if (coll != null) {
			monitor.setRemainingCount(new AtomicLong(coll.count()));// .剩余任务数 TODO
			monitor.setActiveThreadNum((Integer) request.getExtra("activeThreadNum"));
		}
		if (monitor.getFirst()) {
			monitor.setUuid((String) request.getExtra("uuid"));
			monitor.setFirst(false);
		}
	}

	/**
	 * 持久化监控数据。将monitor的数据封装进new出来的MonitroMessage对象，写入mongoDB。
	 */
	private void persistMonitorMessage() {
		
		template.insert(monitor.getErrorMessages(),monitor.getErrorCollectionName());//
		
		monitor.getErrorMessages().removeAll(monitor.getErrorMessages());// 清空监控器异常信息
		MonitorMessage message = new MonitorMessage();// 监控信息实体类
		setBasicMessage(message);
		setSpeed(message);
		template.save(message, monitor.getMonitorCollectionName());
		persistSpeed(monitor.getMonitorId(), monitor.getSpeedInt());
		monitor.setPreviousSuccessCount(new AtomicLong(monitor.getSuccessCount().get()));
		monitor.setPrevious(monitor.getNow());
	}

	/**
	 * 最后一次持久化。
	 */
	private void persistMonitorMessageLast() {//TODO
		template.insert(monitor.getErrorMessages(), monitor.getErrorCollectionName());
		monitor.getErrorMessages().removeAll(monitor.getErrorMessages());// 清空监控器异常信息
		MonitorMessage message = new MonitorMessage();// 监控信息实体类
		setBasicMessage(message);
		template.save(message, monitor.getMonitorCollectionName());
	}
	
	/**
	 * monitor基础监控数据封装，不包含速度信息。
	 * @param message
	 * @param monitor
	 */
	private void setBasicMessage(MonitorMessage message){
		message.setMonitorId(monitor.getMonitorId());
		message.setIp(monitor.getIp());
		message.setUuid(monitor.getUuid());
		message.setActiveThreadNum(monitor.getActiveThreadNum());
		message.setStartTime(monitor.getStartTime());
		message.setSuccessCount(monitor.getSuccessCount().get());
		message.setRemainingCount(monitor.getRemainingCount().get());
		message.setErrorCount(monitor.getErrorCount().get());
		message.setEndTime(monitor.getEndTime());
		message.setState(monitor.getState());
		message.setRemoteAddress(monitor.getRemoteAddress());
		message.setElipseTime(new Date().getTime() - monitor.getStartTime().getTime());
		message.setDownloaderSpend(monitor.getDownloaderSpend() / 1000);
		message.setPageProcessorSpend(monitor.getPageProcessorSpend() / 1000);
		message.setPipelineSpend(monitor.getPipelineSpend() / 1000);
	}
	
	/**
	 * 计算速度，并设置。
	 * @param message
	 */
	private void setSpeed(MonitorMessage message) {
		// 持久化速度信息
		if (monitor.getPreviousSuccessCount() == null || monitor.getPreviousSuccessCount().get() == 0) {//第一次持久化速度
			monitor.setNow(new Date());
			monitor.setSpeedInt((int) monitor.getSuccessCount().get()
				/ ((int) (monitor.getNow().getTime() - monitor.getStartTime().getTime()) / 1000));
		} else {//非第一次持久化速度
			monitor.setNow(new Date());
			monitor.setSpeedInt((int) (monitor.getSuccessCount().get() - monitor.getPreviousSuccessCount().get())
				/ ((int) (monitor.getNow().getTime() - monitor.getPrevious().getTime()) / 1000));
		}
		message.setSpeed(monitor.getSpeedInt());
		
	}
	
	/**
	 * 持久化速度信息
	 * 
	 * @param monitorId 对应监控信息的id
	 * @param speedInt 速度
	 */
	private void persistSpeed(Long monitorId, Integer speedInt) {
		Speed speed = new Speed();
		speed.setDate(new Date());
		speed.setMonitorId(monitorId);
		speed.setSpeed(speedInt);
		template.insert(speed, "speed" + monitorId);
	}

	/**
	 * 最后一次快照。
	 */
	private void snapShotBeforePersistLast() {
		DBCollection coll = template.getCollection(monitor.getCollectionName());
		if (coll != null) {
			monitor.setRemainingCount(new AtomicLong(coll.count()));// .剩余任务数
		}
	}
}
