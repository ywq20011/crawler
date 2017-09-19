package us.codecraft.webmagic.utils;

import java.io.IOException;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;

public class GroovyCommonUtil {
    static String root[]=new String[]{""};  
    static GroovyScriptEngine groovyScriptEngine;  

    static{
        try {
            groovyScriptEngine=new GroovyScriptEngine(root);
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    /**
     *    用于调用指定Groovy脚本中的指定方法 
     * @param scriptName    脚本名称
     * @param methodName    方法名称
     * @param params        方法参数
     * @return
     */
    @SuppressWarnings({ "rawtypes"})
    public static Object invokeMethod(String script, String methodName, Object... params){
        Object ret = null;
        Class scriptClass = null;
        GroovyObject scriptInstance = null;
        try {
        	GroovyClassLoader groovyClassLoader = groovyScriptEngine.getGroovyClassLoader();
        	scriptClass = groovyClassLoader.parseClass(script);
            //scriptClass = groovyScriptEngine.loadScriptByName(scriptName);
            scriptInstance = (GroovyObject)scriptClass.newInstance();
        } catch ( InstantiationException e1) {
            e1.printStackTrace();//此处应输出日志
        }catch(IllegalAccessException e1){
        	e1.printStackTrace();
        }

        try {
            ret = scriptInstance.invokeMethod(methodName, params);
        } catch (IllegalArgumentException e ) {
            e.printStackTrace();//此处应输出日志
        }catch( SecurityException e){
        	e.printStackTrace();//此处应输出日志
        }

        return ret;
    }
}