package com.hlg.webgleaner.core.rmimonitor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Spider;
/**
 * 爬虫远程RMI，用于手动控制爬虫。提供手动启动爬虫和关闭爬虫的功能。单例模式。
 * 拥有一个单例的Map，存放手动调用爬虫时实例化的spider实例对象。
 * 采用java反射机制，提供以下功能：
 *  1)获取list属性里每个对象元素的声明方法名称;
 *  2)根据方法名称调用list属性中对应对象的对应方法（要求两个对象不能有相同的方法名称）;
 *  3)根据爬虫monitorId停止map中对应爬虫运行状态并移除。
 * 
 * @author yangwq
 * @Date 2016年5月23日
 */
public class ManualSpiderRMIImpl extends UnicastRemoteObject implements ManualSpiderRMI {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private static final long serialVersionUID = 1L;

	/**
	 * 存放生产的爬虫
	 */
	private static Map<Long,Spider> manualSpiderMap = new HashMap<Long,Spider>();
	
	private static ManualSpiderRMIImpl manualSpiderRMIImpl;
	
	private static List<Object> spiderFactorys;
	
	private  ManualSpiderRMIImpl(List<Object> spiderFactorys) throws RemoteException {
		super();
		ManualSpiderRMIImpl.spiderFactorys = spiderFactorys;
	}
	
	/**
	 * 获取map的get方法
	 * @return
	 */
	public static Map<Long, Spider> getManualSpiderMap() {
		if(manualSpiderRMIImpl == null) {
			return null;
		}
		return manualSpiderMap;
	}

	/**
	 * 用于获取ManualSpiderRMIImpl远程对象。
	 * 此方法不仅可以获取对象，也用于实例化对象。
	 * 当传入的参数不为null，且属性manualSpiderRMIImpl和spiderFactorys为null，调用构造器实例化manualSpiderRMIImpl。
	 * 当传入的参数为null且manualSpiderRMIImpl不为null，直接返回manualSpiderRMIImpl
	 * 
	 * @param spiderFactorys
	 * @return
	 * @throws RemoteException
	 */
	public static ManualSpiderRMIImpl getInstance(List<Object> spiderFactorys) {
		if(spiderFactorys != null && ManualSpiderRMIImpl.spiderFactorys == null && manualSpiderRMIImpl == null) {
			try {
				manualSpiderRMIImpl = new ManualSpiderRMIImpl(spiderFactorys);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		if(spiderFactorys == null && ManualSpiderRMIImpl.manualSpiderRMIImpl!=null) {
			return manualSpiderRMIImpl;
		}
		return manualSpiderRMIImpl;
		
	}
	
	@Override
	public List<String> getSimpleClassNames() throws RemoteException {
		List<String> list = new ArrayList<String>();
		for(Object spiderFactory : spiderFactorys){
			list.add(spiderFactory.getClass().getSimpleName());
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String,List<String>>  showSpiderMethods() throws RemoteException {
		
		Map<String,List<String>> returnMap = new HashMap<String,List<String>>();
		for(Object spiderFactory : spiderFactorys) {
			List<String> list = new ArrayList<String>();
			Field field = null;
			try {
				field = spiderFactory.getClass().getDeclaredField("descriptions");
				field.setAccessible(true);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				log.error("没有这个字段:{}",e);
			}
			List<String> descriptions = null;
			if(field != null){
				 try {
					descriptions = (List<String>)field.get(spiderFactory);
					Method[] methods = spiderFactory.getClass().getDeclaredMethods();
					String[] methodsString = new String[methods.length];
					for(int i = 0,len = methods.length; i < len; i++) {
						methodsString[i] = methods[i].getName();
					}
					Arrays.sort(methodsString);//排序
					for(int i=0,len = methods.length;i<len;i++) {
						list.add(methodsString[i]+"_"+descriptions.get(i));
					}
					returnMap.put(spiderFactory.getClass().getSimpleName(), list);
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}else{
				Method[] methods = spiderFactory.getClass().getDeclaredMethods();
				for(int i=0,len = methods.length;i<len;i++) {
					list.add(methods[i].getName());
				}
				returnMap.put(spiderFactory.getClass().getSimpleName(), list);
			}
		}
		//map里的spider名称放进去。
		List<String> spiders = new ArrayList<String>(); 
		for(Long monitorId : manualSpiderMap.keySet()) {
			spiders.add(String.valueOf(monitorId));
		}
		returnMap.put("spiders",spiders);
		return returnMap;
	}
	
	@Override
	public void invoke(String methodName) throws RemoteException {
		//注意事项：方法名称应该都不一样。
		for(Object spiderFactory : spiderFactorys) { 
			Method[] methods = spiderFactory.getClass().getDeclaredMethods();
			boolean invoke = false;
			for(Method m : methods){
				if(m.getName().equals(methodName)) {
					invoke = true;
					try {
						Spider spider = (Spider)m.invoke(spiderFactory);
						if(spider !=null){//每台机器设置同一只爬虫只运行一只。TODO
							//manualSpiderMap.put(hostAddress.replace(".", "")+sdf.format(spider.getStartTime()), spider);
							manualSpiderMap.put(spider.getMonitorId(), spider);
							spider.runAsync();
						}
						break;//跳出内层for循环
					} catch (IllegalArgumentException e) {
						log.error("非法参数:{}",e);
					} catch (IllegalAccessException e) {
						log.error("非法访问:{}",e);
					} catch (InvocationTargetException e) {
						log.error("调用目标异常:{}",e);
					}
				}
			}
			if(invoke){//当跳出内层for循环时，方法已经调用，invoke为true，马上跳出外层for循环。
				break;
			}
		}
	}
	
	@Override
	public void removeSpider(String key) {
		Long longkey = Long.valueOf(key);
		if(manualSpiderMap.get(longkey)!=null){
			manualSpiderMap.get(longkey).getStat().set(2);
			manualSpiderMap.remove(longkey);
		}
	}
	
	/**
	 * 重启爬虫
	 * @param key
	 */
	public void reviveSpider(String key) {
		Long longKey = Long.valueOf(key);
		Spider spider = manualSpiderMap.get(longKey);
		if(spider!=null) {
			spider.runAsync();
		}
	}

}
