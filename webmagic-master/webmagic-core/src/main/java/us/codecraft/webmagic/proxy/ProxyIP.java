package us.codecraft.webmagic.proxy;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理IP信息
 * @author linjx
 * @Date 2016年3月26日
 * @Version 1.0.0
 */
public class ProxyIP implements Serializable {
	
	private static final long serialVersionUID = 4201870201926046912L;

	private String ip;
	
	private int port;
	
	private String country;
	
	private String location;
	
	/**
	 * 类型：透明（Transparent）
	 * 匿名（Anonymous）
	 * 混淆（Distorting)
	 * 高匿（Elite）
	 * @see http://blog.csdn.net/a19860903/article/details/47146715
	 */
	private ProxyLeverType type = ProxyLeverType.Elite;  
	
	/**
	 * 是否代理https
	 */
	private Boolean isHttps = false;
	
	/**
	 * 验证时间
	 */
	private Date verifyTime;
	
	
	enum ProxyLeverType {
		Transparent, 
		Anonymous,
		Distorting,
		Elite;
	}
	
	public ProxyIP() {}
	
	public ProxyIP(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public ProxyIP(String ip, int port, boolean isHttps) {
		this(ip, port);
		this.isHttps = isHttps;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public ProxyLeverType getType() {
		return type;
	}

	public void setType(ProxyLeverType type) {
		this.type = type;
	}

	public Boolean getIsHttps() {
		return isHttps;
	}

	public void setIsHttps(Boolean isHttps) {
		this.isHttps = isHttps;
	}

	public Date getVerifyTime() {
		return verifyTime;
	}

	public void setVerifyTime(Date verifyTime) {
		this.verifyTime = verifyTime;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
}
