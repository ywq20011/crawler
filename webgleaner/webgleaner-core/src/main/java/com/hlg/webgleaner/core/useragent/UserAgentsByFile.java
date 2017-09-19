package com.hlg.webgleaner.core.useragent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.hlg.webgleaner.core.utils.RandomGenerator;

/**
 * UserAgent管理
 * 
 * @author linjx
 * @Date 2016年3月24日
 * @Version 1.0.0
 */
public class UserAgentsByFile implements UserAgentStrategy {

	private String uaFilePath;

	private static final List<UserAgentBo> userAgents = new ArrayList<UserAgentsByFile.UserAgentBo>();
	private AtomicInteger atomicInt = new AtomicInteger();

	public UserAgentsByFile(String uaFilePath) throws IOException {
		this.uaFilePath = uaFilePath;
		init();
	}
	

	/**
	 * 随机获取userAgent
	 * 
	 * @return
	 */
	@Override
	public String getRandomUserAgent() {
		if (CollectionUtils.isEmpty(userAgents)) {
			return UserAgentStrategy.DEFAULT_UA;
		}
		UserAgentBo bo = userAgents.get(RandomGenerator.randomInt(1000, 0) % userAgents.size());
		bo.getAtomicInt().getAndIncrement();
		return bo.getUserAgent();
	}

	/**
	 * 轮询方式获取UserAgent
	 * 
	 * @return
	 */
	@Override
	public String getRotationUserAgent() {
		if (CollectionUtils.isEmpty(userAgents)) {
			return UserAgentStrategy.DEFAULT_UA;
		}
		UserAgentBo bo = userAgents.get(atomicInt.getAndIncrement() % userAgents.size());
		bo.getAtomicInt().getAndIncrement();
		return bo.getUserAgent();
	}

	@Override
	public String getAllUsedState() {
		StringBuilder sb = new StringBuilder();
		for (UserAgentBo bo : userAgents) {
			sb.append(bo.toString()).append("\n");
		}
		return sb.toString();
	}
	

	private void init() throws IOException {
		File file = new File(uaFilePath);
		if (!file.exists()) {
			throw new RuntimeException("The ua file is not exist.");
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (StringUtils.isNotBlank(line)) {
					userAgents.add(new UserAgentBo(StringUtils.trim(line), new AtomicInteger()));
				}
			}
			reader.close();
		} catch (IOException e) {
			throw e;
		} finally {
			if (null != reader) {
				reader.close();
			}
		}
	}
	
	class UserAgentBo {
		String userAgent;
		AtomicInteger atomicInt;
		public UserAgentBo(String ua, AtomicInteger atomicInt) {
			this.userAgent = ua;
			this.atomicInt = atomicInt;
		}
		public String getUserAgent() {
			return userAgent;
		}
		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
		public AtomicInteger getAtomicInt() {
			return atomicInt;
		}
		public void setAtomicInt(AtomicInteger atomicInt) {
			this.atomicInt = atomicInt;
		}
		@Override
		public String toString() {
			return String.format(">>>used-count:%d >>>user-agent:%s", atomicInt.get(), userAgent);
		}
	}

}
