package com.hlg.webgleaner.core.useragent;

/**
 * 用户代理接口类<br>
 * 策略模式
 * @author linjx
 * @Date 2016年3月28日
 * @Version 1.0.0
 */
public interface UserAgentStrategy {
	
	public static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
	
	/**
	 * 随机获取用户代理
	 * @return
	 */
	public String getRandomUserAgent();

	/**
	 * 轮询获取用户代理
	 * @return
	 */
	public String getRotationUserAgent();
	
	/**
	 * 轮询并循环使用用户代理（循环是为了满足在一个路径下保持多次使用）
	 * @return
	 */
	//public String getRotationLoopUserAgent();
	
	/**
	 * 获取UA使用情况
	 * @return
	 */
	public String getAllUsedState();
	
}
