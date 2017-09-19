package com.hlg.webgleaner.core.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean工具类
 * @author linjx
 * @Date 2016年2月23日
 * @Version 1.0.0
 */
public abstract class BeanUtils {
	
	/**
	 * 将Map转换为Bean
	 * @param map
	 * @param targetClass
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public static <T> T convertMap2Bean(Map<String, Object> map, Class<T> targetClass) 
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		if (MapUtils.isEmpty(map)) {
			return null;
		}
		T bean = targetClass.newInstance();
		org.apache.commons.beanutils.BeanUtils.populate(bean, map);
		return bean;
	}
	
	/**
	 * 转换Map数组至Bean数组
	 * @param mapList
	 * @param targetClass
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static <T> List<T> convertMapList2BeanList(List<Map<String, Object>> mapList, Class<T> targetClass) 
			throws InstantiationException, IllegalAccessException, InvocationTargetException {
		if (CollectionUtils.isEmpty(mapList)) {
			return null;
		}
		List<T> beanList = new ArrayList<T>(mapList.size());
		for (Map<String, Object> map : mapList) {
			T bean = convertMap2Bean(map, targetClass);
			beanList.add(bean);
		}
		
		return beanList;
	}
	
	/**
	 * 将Bean转换为Map
	 * @param bean
	 * @throws IntrospectionException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public static Map<String, Object> convertBean2Map(Object bean) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (null == bean) {
			return null;
		}
		Map<String, Object> resultMap = new HashMap<String, Object>();
		BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		if (ArrayUtils.isNotEmpty(propertyDescriptors)) {
			for (PropertyDescriptor property : propertyDescriptors) {
				String fieldName = property.getName();
				if (!StringUtils.equals(fieldName, "class")) {
					Method getter = property.getReadMethod();
					Object value = getter.invoke(bean);
					
					resultMap.put(fieldName, value);
				}
			}
		}
		
		return resultMap;
	}

}
