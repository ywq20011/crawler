package com.hlg.webgleaner.core.listener.bo;

import org.springframework.data.annotation.Id;

/**
 * 用于封装超过重试次数的任务信息，持久化至mongoDB。
 * 
 * @author yangwq
 */
public class FailedUrlBo {

	@Id
	private String url;

	/**
	 * 重试次数
	 */
	private Integer retryTimes;

	public Integer getRetryTimes() {
		return retryTimes;
	}

	public void setRetryTimes(Integer retryTimes) {
		this.retryTimes = retryTimes;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
