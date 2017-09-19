package com.hlg.webgleaner.core.listener;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;

import com.hlg.webgleaner.core.listener.bo.Monitor;
import com.hlg.webgleaner.core.listener.utils.MailUtil;
import com.hlg.webgleaner.core.rmimonitor.ManualSpiderRMIImpl;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.rmi.QuartzSpiderRMIImpl;

/**
 * 爬虫运行纠错工具。主要功能为：
 * 爬虫剩余任务数量为0时确保爬虫退出：少数爬虫可能任务结束后跳不出while循环，出现僵尸线程；
 * 爬虫剩余任务数量不为0，成功次数和失败次数不变，爬虫线程被终止（原因不好说，可能是因为内存溢出），这时重启爬虫；
 * 爬虫剩余任务数量不为0，成功次数不增长，而错误次数增长时做出提醒
 * （可能的原因：1.爬虫被墙；
 * 			  2.网络中断；
 * 			  3.爬虫运行到收尾阶段，剩余任务都是经过重试机制被剩下的，这时可能会做此提醒，但属于正常情况
 * 			  4...	）。
 * 另外带有日志记录功能，分别记录自动爬虫和手动爬虫前20条检测记录。
 * 使用时，在爬虫入口程序上进行rmi注册。
 * 
 * @author yangwq
 * @Date 2016年6月6日
 */
public class WebgleanerMonitorCorrector extends UnicastRemoteObject implements WebgleanerMonitorCorrectorRemote {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 自动爬虫Map实例。单实例。
	 */
	private Map<Long, Spider> autoSpiderMap = QuartzSpiderRMIImpl.getAutoSpiderMap();
	
	/**
	 * 手动爬虫Map单实例。
	 */
	private	static Map<Long, Spider> manualSpiderMap = ManualSpiderRMIImpl.getManualSpiderMap();
	 
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 自动爬虫纠错日志存储，单实例，最多存储20条日志。
	 */
	private static LimitQueue<String> autoLogList = new LimitQueue<String>(20); 
	
	/**
	 * 手动爬虫纠错日志存储，但实例，最多存储20条日志。
	 */
	private static LimitQueue<String> manualLogList = new LimitQueue<String>(20);
	
	/**
	 * 远程单例对象
	 */
	private static WebgleanerMonitorCorrector corrector;
	
	private WebgleanerMonitorCorrector() throws RemoteException {}
	
	public static WebgleanerMonitorCorrector getInstance() {
		if(corrector == null) {
			try {
				corrector = new WebgleanerMonitorCorrector();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return corrector;
	}
	
	/**
	 * 远程方法，用于获取自动爬虫纠错日志队列。
	 * @return
	 */
	public LimitQueue<String> getAutoLogList() {
		return autoLogList;
	}
	
	/**
	 * 远程方法，用于获取手动爬虫纠错日志队列
	 */
	public LimitQueue<String> getManualLogList() {
		return manualLogList;
	}
	
	/**
	 * 检测当前是否有运行中的爬虫
	 * @return
	 * @throws RemoteException
	 */
	public boolean checkAutoSpidersInMap() throws RemoteException {
		if(autoSpiderMap.size()>0) {
			autoLogList.offer(sdf.format(new Date())+"当前机器有"+autoSpiderMap.size()+"只自动爬虫正在运行，开始检测自动爬虫运行状态...");
			return true;
		}
		autoLogList.offer(sdf.format(new Date()) + "当前没有自动爬虫运行，跳过检测...");
		return false;
	}
	
	/**	
	 *  此方法为二次检查，在爬虫调用onClose()时其实已经移除了任务结束的爬虫，但是少数爬虫陷入while循环出不来，此方法可以检测出这些爬虫并关闭移除。
	 * @throws RemoteException 
	 */
	public void checkToRemoveAutoSpiders() throws RemoteException {
		//遍历map，检查。
		Iterator<Entry<Long, Spider>> it = autoSpiderMap.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToRemoveSpider(monitor)) {//
						entry.getValue().setStat(new AtomicInteger(2));
						it.remove();
						autoLogList.offer(sdf.format(new Date())+entry.getKey()+">>>自动爬虫任务结束，销毁自动爬虫...>>>当前机器剩余"+autoSpiderMap.size()+"只自动爬虫...");
					}
				}
			}
		}
	}
	
	/**
	 * 爬虫出现内存问题时，任务退出，remainingcount>0,其他属性停止变化，这时重启爬虫。
	 * @throws RemoteException 
	 */
	public void checkToRestartAutoSpiders() throws RemoteException {
		//遍历map，检查。
		Iterator<Entry<Long, Spider>> it = autoSpiderMap.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToRestart(monitor)) {//TODO
						autoLogList.offer(sdf.format(new Date())+entry.getKey()+">>>error:自动爬虫异常停止工作，重启自动爬虫...");
						entry.getValue().runAsync();
					}
				}
			}
		}
	}

	/**
	 * 当爬虫的成功次数不增长，错误次数增长时，可能是网路问题，停止爬虫，。
	 * @throws RemoteException 
	 */
	public void checkToStopAutoSpiderAndEmail() throws RemoteException {
		//遍历map，检查。
		Iterator<Entry<Long, Spider>> it = autoSpiderMap.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToStopAndSendEmail(monitor)) {//
						autoLogList.offer(sdf.format(new Date())+entry.getKey()+">>>error:自动爬虫正在运行，但是没有有效爬取数据，请检查.");
						sendEmail();//发送邮件。
						entry.getValue().setStat(new AtomicInteger(2));//关闭爬虫
						it.remove();//移除爬虫。
					} else {
						autoLogList.offer(sdf.format(new Date()) + entry.getKey()+">>>本轮检测正常...");
					}
						monitor.setCheckSuccessCount(monitor.getSuccessCount().get());
						monitor.setCheckErrorCount(monitor.getErrorCount().get());
				}
			}
		}
	}
	
	/**
	 * 检测当前是否启动了手动爬虫。
	 * @throws RemoteException 
	 */
	public boolean checkManualSpidersInMap() throws RemoteException {
		if(manualSpiderMap.size()>0) {
			manualLogList.offer(sdf.format(new Date())+"当前机器有"+manualSpiderMap.size()+"只手动爬虫正在运行，开始检测手动爬虫运行状态...");
			return true;
		}
		manualLogList.offer(sdf.format(new Date())+"当前没有手动爬虫运行，跳过检测...");
		return false;
	}

	/**
	 * 检测是否移除手动爬虫
	 * @throws RemoteException
	 */
	public void checkToRemoveManualSpiders() throws RemoteException {
		Iterator<Entry<Long, Spider>> it = manualSpiderMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToRemoveSpider(monitor)) {//
						entry.getValue().setStat(new AtomicInteger(2));
						it.remove();
						manualLogList.offer(sdf.format(new Date())+entry.getKey()+"手动爬虫任务结束，销毁手动爬虫...>>>当前机器剩余"+manualSpiderMap.size()+"只手动爬虫...");
					}
				}
			}
		}
	}

	/**
	 * 检测是否重启手动爬虫
	 * @throws RemoteException 
	 */
	public void checkToRestartManualSpiders() throws RemoteException {
		Iterator<Entry<Long, Spider>> it = manualSpiderMap.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToRestart(monitor)) {
						manualLogList.offer(sdf.format(new Date())+entry.getKey()+">>>error:手动爬虫异常停止工作，重启手动爬虫...");
						entry.getValue().runAsync();
					}
				}
			}
		}
	}

	/**
	 * 检测是否需要停止爬虫并发送邮件通知
	 * @throws RemoteException
	 */
	public void checkToStopManualSpiderAndEmail() throws RemoteException {
		Iterator<Entry<Long, Spider>> it = manualSpiderMap.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, Spider> entry = it.next();
			List<SpiderListener> listeners = entry.getValue().getSpiderListeners();
			for(SpiderListener listener : listeners) {
				if(listener instanceof WebgleanerMonitorListener) {
					WebgleanerMonitorListener eclistener = (WebgleanerMonitorListener) listener;
					Monitor monitor = eclistener.getMonitor();
					if(needToStopAndSendEmail(monitor)) {//
						manualLogList.offer(sdf.format(new Date())+entry.getKey()+">>>error:手动爬虫正在运行，但是没有有效爬取数据，请检查。");
						sendEmail();
						entry.getValue().setStat(new AtomicInteger(2));//关闭爬虫
						it.remove();
					} else {
						manualLogList.offer(sdf.format(new Date())+entry.getKey()+">>>本轮检测正常...");
					}
					monitor.setCheckSuccessCount(monitor.getSuccessCount().get());
					monitor.setCheckErrorCount(monitor.getErrorCount().get());
				}
			}
		}
	}

	/**
	 * 发送邮件通知，爬虫遭遇重大问题，成功次数不增加，失败次数增加，需要人工检查。
	 */
	private void sendEmail() {
		//TODO
		List<String> list = new ArrayList<String>();
		list.add("yangwq@linesum.com");
		list.add("fucl@linesum.com");
		list.add("linjx@linesum.com");
		MailUtil.sendMail(
				"smtp.139.com",
		         "25",
		         true,
		         "13859745260@139.com",
		         "5581653jfigu",
		         list,
		         "爬虫运行异常警告",
		         "尊敬的用户：您的爬虫于"+new Date()+"发生异常,已经停止并移除爬虫,请检查异常后手动重启爬虫！"
		        , true, true);
	}

	/**
	 * 判断是否需要重启爬虫.如果爬虫剩余次数大于0，而且成功次数与上一次成功次数一样（没有增加），
	 * 并且失败次数也没有增加，说明爬虫处于静止状态，已经停止工作，需要重启爬虫。
	 * 最多重启3次。
	 * @param monitor
	 * @return
	 */
	private boolean needToRestart(Monitor monitor) {
		if(!monitor.getFirst() && monitor.getRemainingCount().get()>0 && 
			monitor.getSuccessCount().get() == monitor.getCheckSuccessCount() &&
			monitor.getErrorCount().get() == monitor.getCheckErrorCount()) {
			monitor.setRestartTime(monitor.getRestartTime()+1);
			if(monitor.getRestartTime()>3) {//重启超过三次就不要重启了。是吗？TODO
				return false;
			}
			return true;	
		}
		return false;
	}
	
	/**
	 * 剩余任务数大于0，成功次数不变，失败次数增加，说明爬虫再进行无效爬取，超过三次则停止爬虫。
	 * 提供三次容错。
	 * @param monitor
	 * @return
	 */
	private boolean needToStopAndSendEmail(Monitor monitor) {
		if(!monitor.getFirst() && monitor.getRemainingCount().get() > 0 &&
			monitor.getSuccessCount().get() == monitor.getCheckSuccessCount() &&
			monitor.getErrorCount().get() > monitor.getCheckErrorCount()) {
			monitor.setThreadhold(monitor.getThreadhold()+1);
		}
		if(monitor.getThreadhold()>2) {
			return true;//超过三次直接停止爬虫并移除爬虫，发送邮件。提供三次容错，说不定是因为网络问题，过会就好了。
		}
		return false;
	}

	/**
	 * 非初次记录，剩余任务数为0，成功数大于0，说明此爬虫任务已经结束，可以移除。
	 * 初次记录未进行，状态为结束，说明爬虫任务可能很少，甚至为0，已经结束，可以移除。
	 * 此方法为二次检查，在爬虫close时其实已经移除了任务结束的爬虫，但是少数爬虫陷入while循环出不来，此方法可以检测出这些爬虫。
	 * @param monitor
	 * @return
	 */
	private boolean needToRemoveSpider(Monitor monitor) {
		if(!monitor.getFirst() && monitor.getRemainingCount().get() == 0 && monitor.getSuccessCount().get() > 0) {
			return true;//已经结束的
		}else if(monitor.getState() == null) {//第一次爬取还没进行
			return false;
		}else if(monitor.getFirst() && "stopped".equals(monitor.getState())) {
			return true;//一开始剩余任务数就为0的。
		}else if(monitor.getRestartTime()>3) {//重启次数超过三次的
			return true;
		}
		return false;
	}

	/**
	 * 获取自动爬虫的map
	 * @return
	 */
	public Map<Long, Spider> getAutoSpiderMap() {
		return autoSpiderMap;
	}

	/**
	 * 获取手动爬虫的map
	 * @return
	 */
	public Map<Long, Spider> getManualSpiderMap() {
		return manualSpiderMap;
	}
	
	/**
	 * 开启自动化纠错,在客户端入口程序调用。
	 * 以守护线程方式启动纠错组件。
	 */
	public static void startCorrector() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				Timer timer =  new Timer();
				final WebgleanerMonitorCorrector corrector = WebgleanerMonitorCorrector.getInstance();
				timer.schedule(new TimerTask(){
					@Override
					public void run() {
						try {
							if(corrector.checkAutoSpidersInMap()){
								corrector.checkToRemoveAutoSpiders();
								corrector.checkToRestartAutoSpiders();
								corrector.checkToStopAutoSpiderAndEmail();
							}
							if(corrector.checkManualSpidersInMap()){
								corrector.checkToRemoveManualSpiders();
								corrector.checkToRestartManualSpiders();
								corrector.checkToStopManualSpiderAndEmail();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}, 60*2000,60*2000);//每1分钟检测一次。
			}
		});
		thread.setDaemon(true);//守护线程
		thread.start();//启动线程
	}
}
