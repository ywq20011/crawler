package com.hlg.webgleaner.core.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;

/**
 * 模板解析器
 * @author linjx
 * @Date 2016年3月1日
 * @Version 1.0.0
 */
public abstract class TemplateParser {

	/**
     * 替换模板变量
     * 
     * @param template
     * @param dataMap
     * @return
     */
    public static String replaceArgs(String template, Map<String, Object> dataMap){
    	if (MapUtils.isEmpty(dataMap)) {
    		return template;
    	}
        // sb用来存储替换过的内容，它会把多次处理过的字符串按源字符串序 存储起来。
        StringBuffer sb = new StringBuffer();
        try{
            Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(template);
            while(matcher.find()){
                String name = matcher.group(1);// 键名
                String value = String.valueOf(dataMap.get(name));// 键值
                if(value == null){
                    value = "";
                }else{
                    value = value.replaceAll("\\$", "\\\\\\$");
                }
                matcher.appendReplacement(sb, value);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return sb.toString();   //加一个空行（结束行）
    }
	
}
