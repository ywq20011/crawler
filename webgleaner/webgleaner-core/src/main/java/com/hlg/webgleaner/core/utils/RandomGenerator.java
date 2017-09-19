package com.hlg.webgleaner.core.utils;

/**
 * 随机生成器 
 * @author linjx
 * @Date 2016年3月24日
 * @Version 1.0.0
 */
public class RandomGenerator {
	
	/**
	 * 
	 * @param max
	 * @param min
	 * @return
	 */
	public static int randomInt(int max, int min) {
		//Math.random()产生0-1白噪声
		return (int) ((min + Math.random())*(max - min + 1));
	}

}
