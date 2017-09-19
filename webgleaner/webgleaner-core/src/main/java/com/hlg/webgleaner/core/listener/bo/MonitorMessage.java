package com.hlg.webgleaner.core.listener.bo;

import java.util.Date;

import org.springframework.data.annotation.Id;

/**
 * 用于封装爬虫运行状态信息。持久化至数据库。
 * 
 * @author yangwq
 * @Date 2016年5月16
 */
public class MonitorMessage {

	/**
	 * 速度
	 */
	private Integer speed;
	/**
	 * 成功任务次数
	 */
	private Long successCount;

	/**
	 * 失败任务次数
	 */
	private Long errorCount;

	/**
	 * 剩余任务次数
	 */
	private Long remainingCount;

	/**
	 * threadAliveNum
	 */
	private Integer activeThreadNum;

	/**
	 * spider启动时间
	 */
	private Date startTime;

	/**
	 * spider结束时间
	 */
	private Date endTime;

	/**
	 * 所监控spider的uuid
	 * 
	 * @return
	 */
	private String uuid;
	/**
	 * 唯一标示当前监听记录。由ip和时间搓做成。
	 */
	@Id
	private Long monitorId;

	/**
	 * 当前机器IP
	 */
	private String ip;

	/**
	 * 所监控的爬取类型，目前分为分为京东，淘宝天猫店铺，淘宝商品，天猫商品
	 * 
	 * @return
	 */
	private SpiderType spiderType;

	/**
	 * 状态
	 */
	private String state;

	/**
	 * 已耗时
	 */
	private Long elipseTime;
	/**
	 * downloader总耗时
	 */
	private Long downloaderSpend;
	/**
	 * pageProcessor总耗时
	 */
	private Long pageProcessorSpend;

	/**
	 * pipeline总耗时
	 */
	private Long pipelineSpend;

	/**
	 * 远程地址 
	 */
	private String remoteAddress;
	
	/**
	 * 搜索id
	 */
	private String searchId;
	
	public String getSearchId() {
		return searchId;
	}

	public void setSearchId(String searchId) {
		this.searchId = searchId;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public Long getPipelineSpend() {
		return pipelineSpend;
	}

	public void setPipelineSpend(Long pipelineSpend) {
		this.pipelineSpend = pipelineSpend;
	}

	public Long getDownloaderSpend() {
		return downloaderSpend;
	}

	public void setDownloaderSpend(Long downloaderSpend) {
		this.downloaderSpend = downloaderSpend;
	}

	public Long getPageProcessorSpend() {
		return pageProcessorSpend;
	}

	public void setPageProcessorSpend(Long pageProcessorSpend) {
		this.pageProcessorSpend = pageProcessorSpend;
	}

	public Long getElipseTime() {
		return elipseTime;
	}

	public void setElipseTime(Long elipseTime) {
		this.elipseTime = elipseTime;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public SpiderType getSpiderType() {
		return spiderType;
	}

	public void setSpiderType(SpiderType spiderType) {
		this.spiderType = spiderType;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Long getRemainingCount() {
		return remainingCount;
	}

	public Long getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(Long monitorId) {
		this.monitorId = monitorId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Integer getSpeed() {
		return speed;
	}

	public void setSpeed(Integer speed) {
		this.speed = speed;
	}

	public Long getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(Long successCount) {
		this.successCount = successCount;
	}

	public Long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Long errorCount) {
		this.errorCount = errorCount;
	}

	public void setRemainingCount(Long remaingCount) {
		this.remainingCount = remaingCount;
	}

	public Integer getActiveThreadNum() {
		return activeThreadNum;
	}

	public void setActiveThreadNum(Integer activeThreadNum) {
		this.activeThreadNum = activeThreadNum;
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

}
