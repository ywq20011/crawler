package com.hlg.webgleaner.core.utils;

import com.hlg.webgleaner.core.useragent.UserAgentStrategy;

/**
 * 用户代理UserAgent获取器<br>
 * 策略模式
 * @author linjx
 * @Date 2016年3月29日
 * @Version 1.0.0
 */
public class UserAgentContext {
	
	static final String DEFAULT_FILE = "";
	static final String DEFAULT_COLL = "userAgent";
	
	private UserAgentStrategy userAgents;
	
	public UserAgentContext(UserAgentStrategy userAgents) {
		this.userAgents = userAgents;
	}
	
	public String getRandomUserAgent() {
		return userAgents.getRandomUserAgent();
	}

	public String getRotationUserAgent() {
		return userAgents.getRotationUserAgent();
	}
	
	public String getAllUsedState() {
		return userAgents.getAllUsedState();
	}

}
