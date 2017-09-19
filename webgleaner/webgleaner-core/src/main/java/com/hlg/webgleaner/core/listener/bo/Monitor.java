package com.hlg.webgleaner.core.listener.bo;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于记录维护爬虫运行状态信息。当监控数据持久化时机到来时，将内部数据封装进MonitorMessage类。
 * 
 * @author yangwq
 */
public class Monitor {
	
	/**
	 * 是否是首次记录监控数据
	 */
	private boolean first = true;
	
	/**
	 * 所监控爬虫scheduler分发任务的collection名称
	 */
	private String collectionName = null;
	/**
	 * 爬虫所在机器IP地址
	 */
	private String ip = null;
	
	/**
	 * 记录监控信息的collection名称
	 */
	private String monitorCollectionName = null;
	/**
	 * 记录监控异常信息的collection名称
	 */
	private String errorCollectionName = null;
	
	/**
	 * 成功次数
	 */
	private AtomicLong successCount = new AtomicLong(0);
	
	/**
	 * 失败次数
	 */
	private AtomicLong errorCount = new AtomicLong(0);
	
	/**
	 * 开始时间
	 */
	private Date startTime = null;
	
	/**
	 * 结束时间
	 */
	private Date endTime = null;
	
	/**
	 * 剩余任务数
	 */
	private AtomicLong remainingCount = new AtomicLong(0);
	
	/**
	 * 活跃线程数
	 */
	private Integer activeThreadNum = null;
	/**
	 * 异常信息集合
	 */
	private Set<ErrorMessage> errorMessages = Collections.synchronizedSet(new HashSet<ErrorMessage>());
	
	/**
	 * uuid
	 */
	private String uuid = null;
	
	/**
	 * 上次记录前成功数，用于爬取速度计算
	 */
	private AtomicLong previousSuccessCount = null;
	
	/**
	 * 本次记录时间
	 */
	private Date now;
	
	/**
	 * 上次记录时间
	 */
	private Date previous;

	/**
	 * downloader总耗时
	 */
	private long downloaderSpend;
	
	/**
	 * pageProcessor总耗时
	 */
	private long pageProcessorSpend;

	/**
	 * pipeline总耗时
	 */
	private long pipelineSpend;

	/**
	 * 监控对应爬虫绑定的远程地址
	 */
	private String remoteAddress;
	
	/**
	 * 无效爬取检测阈值
	 */
	private int threadhold;
	
	/**
	 * 上次检测时成功次数 
	 */
	private long checkSuccessCount;
	
	/**
	 * 上次检测时失败次数
	 */
	private long checkErrorCount;
	
	/**
	 * 重启次数
	 */
	private int restartTime;
	
	private Long monitorId;
	
	public Long getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(Long monitorId) {
		this.monitorId = monitorId;
	}

	public int getRestartTime() {
		return restartTime;
	}

	public void setRestartTime(int restartTime) {
		this.restartTime = restartTime;
	}

	public long getCheckSuccessCount() {
		return checkSuccessCount;
	}

	public void setCheckSuccessCount(long checkSuccessCount) {
		this.checkSuccessCount = checkSuccessCount;
	}

	public long getCheckErrorCount() {
		return checkErrorCount;
	}

	public void setCheckErrorCount(long checkErrorCount) {
		this.checkErrorCount = checkErrorCount;
	}

	public int getThreadhold() {
		return threadhold;
	}

	public void setThreadhold(int threadhold) {
		this.threadhold = threadhold;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public long getPipelineSpend() {
		return pipelineSpend;
	}

	public void setPipelineSpend(long pipelineSpend) {
		this.pipelineSpend = pipelineSpend;
	}

	public final long getDownloaderSpend() {
		return downloaderSpend;
	}

	public void setDownloaderSpend(long downloaderSpend) {
		this.downloaderSpend = downloaderSpend;
	}

	public long getPageProcessorSpend() {
		return pageProcessorSpend;
	}

	public void setPageProcessorSpend(long pageProcessorSpend) {
		this.pageProcessorSpend = pageProcessorSpend;
	}

	public Date getNow() {
		return now;
	}

	public void setNow(Date now) {
		this.now = now;
	}

	public Date getPrevious() {
		return previous;
	}

	public void setPrevious(Date previous) {
		this.previous = previous;
	}

	public AtomicLong getPreviousSuccessCount() {
		return previousSuccessCount;
	}

	public void setPreviousSuccessCount(AtomicLong previousSuccessCount) {
		this.previousSuccessCount = previousSuccessCount;
	}

	/**
	 * 爬虫状态，running或stop
	 */
	private String state = null;
	/**
	 * 爬取速度
	 */
	private Integer speedInt = null;

	public boolean getFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMonitorCollectionName() {
		return monitorCollectionName;
	}

	public void setMonitorCollectionName(String monitorCollectionName) {
		this.monitorCollectionName = monitorCollectionName;
	}

	public String getErrorCollectionName() {
		return errorCollectionName;
	}

	public void setErrorCollectionName(String errorCollectionName) {
		this.errorCollectionName = errorCollectionName;
	}

	public AtomicLong getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(AtomicLong successCount) {
		this.successCount = successCount;
	}

	public AtomicLong getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(AtomicLong errorCount) {
		this.errorCount = errorCount;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public AtomicLong getRemainingCount() {
		return remainingCount;
	}

	public void setRemainingCount(AtomicLong remainingCount) {
		this.remainingCount = remainingCount;
	}

	public Integer getActiveThreadNum() {
		return activeThreadNum;
	}

	public void setActiveThreadNum(Integer activeThreadNum) {
		this.activeThreadNum = activeThreadNum;
	}

	public Set<ErrorMessage> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(Set<ErrorMessage> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Integer getSpeedInt() {
		return speedInt;
	}

	public void setSpeedInt(Integer speedInt) {
		this.speedInt = speedInt;
	}

}
