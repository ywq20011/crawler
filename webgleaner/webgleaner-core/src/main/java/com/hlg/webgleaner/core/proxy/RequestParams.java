package com.hlg.webgleaner.core.proxy;

public class RequestParams {
	private String tid; // 订单
	private String num; // 数量
	private String operator = "1,2,3";
	private String protocol;
	private String delay = "5"; // 传入 5 表示提取延迟5秒内的代理

	public RequestParams() {
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getNum() {
		return num;
	}

	public void setNum(String num) {
		this.num = num;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getDelay() {
		return delay;
	}

	public void setDelay(String delay) {
		this.delay = delay;
	}
}