package com.hlg.webgleaner.core.listener.bo;

import java.util.Date;

/**
 * 用于封装爬虫速度信息，持久化至数据库。
 * 
 * @author yangwq
 */
public class Speed {

	/**
	 * 所属爬虫监控id，根据此id关联到对应monitorMessage。
	 */
	private Long monitorId;
	/**
	 * 速度值
	 */
	private Integer speed;

	/**
	 * 速度时间点
	 */
	private Date date;

	public Integer getSpeed() {
		return speed;
	}

	public void setSpeed(Integer speed) {
		this.speed = speed;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(Long monitorId) {
		this.monitorId = monitorId;
	}

}
