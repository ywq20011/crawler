package com.hlg.webgleaner.core.listener.bo;

import java.util.Date;

import org.springframework.data.annotation.Id;

/**
 * 爬虫监控的异常集合类，用于封装异常信息。
 * 
 * @author yangwq
 */
public class ErrorMessage {
	
	@Id
	private Long id;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 
	 */
	private String errorUrl;

	/**
	 * 异常发生时间
	 */
	private Date errorTime;

	/**
	 * 异常信息
	 */
	private String exceptionMessage;

	public String getErrorUrl() {
		return errorUrl;
	}

	public void setErrorUrl(String errorUrl) {
		this.errorUrl = errorUrl;
	}

	public Date getErrorTime() {
		return errorTime;
	}

	public void setErrorTime(Date errorTime) {
		this.errorTime = errorTime;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

}
