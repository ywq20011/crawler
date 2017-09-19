
package us.codecraft.webmagic;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.pipeline.CollectorPipeline;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.pipeline.ResultItemsCollectorPipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.rmi.QuartzSpiderRMIImpl;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.scheduler.Scheduler;
import us.codecraft.webmagic.thread.CountableThreadPool;
import us.codecraft.webmagic.utils.GroovyCommonUtil;
import us.codecraft.webmagic.utils.IdUtils;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * Entrance of a crawler.<br>
 * A spider contains four modules: Downloader, Scheduler, PageProcessor and
 * Pipeline.<br>
 * Every module is a field of Spider. <br>
 * The modules are defined in interface. <br>
 * You can customize a spider with various implementations of them. <br>
 * Examples: <br>
 * <br>
 * A simple crawler: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")).run();<br>
 * <br>
 * Store results to files by FilePipeline: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .pipeline(new FilePipeline("/data/temp/webmagic/")).run(); <br>
 * <br>
 * Use FileCacheQueueScheduler to store urls and cursor in files, so that a
 * Spider can resume the status when shutdown. <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .scheduler(new FileCacheQueueScheduler("/data/temp/webmagic/cache/")).run();
 * <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see Downloader
 * @see Scheduler
 * @see PageProcessor
 * @see Pipeline
 * @since 0.1.0
 */
public class Spider implements Runnable, Task {

	protected Downloader downloader;
	
	protected Long monitorId;
	
	public Long getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(Long monitorId) {
		this.monitorId = monitorId;
	}

	protected List<Pipeline> pipelines = new ArrayList<Pipeline>();

	public List<Pipeline> getPipelines() {
		return pipelines;
	}

	protected PageProcessor pageProcessor;

	protected List<Request> startRequests;

	protected Site site;

	protected String uuid;

	protected Scheduler scheduler = new QueueScheduler();// 默认的

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected CountableThreadPool threadPool;

	protected ExecutorService executorService;

	protected int threadNum = 1;

	protected AtomicInteger stat = new AtomicInteger(STAT_INIT);

	protected boolean exitWhenComplete = true;

	protected final static int STAT_INIT = 0;

	protected final static int STAT_RUNNING = 1;

	protected final static int STAT_STOPPED = 2;

	protected boolean spawnUrl = true;

	protected boolean destroyWhenExit = true;

	private ReentrantLock newUrlLock = new ReentrantLock();

	private Condition newUrlCondition = newUrlLock.newCondition();

	private List<SpiderListener> spiderListeners;

	private final AtomicLong pageCount = new AtomicLong(0);

	private Date startTime;

	private int emptySleepTime = 30000;
	
	/**
	 * 是否放入autoSpiderMap，默认为false，不放入。通过quartz任务调用的才放入。
	 */
	private boolean regist = false;
	
	/**
	 * 是否是沙箱测试
	 */
	private boolean isTest;
	
	/**
	 * 沙箱测试结果集
	 */
	private Map<Long,List<String>> testResultMap;
	
	public boolean getIsTest() {
		return isTest;
	}

	/**
	 * create a spider with pageProcessor.
	 *
	 * @param pageProcessor
	 *            pageProcessor
	 * @return new spider
	 * @see PageProcessor
	 */
	public static Spider create(PageProcessor pageProcessor) {
		return new Spider(pageProcessor,false);
	}


	public static Spider create(PageProcessor pageProcessor,boolean isTest,Map<Long,List<String>> testResultMap) {
		return new Spider(pageProcessor,isTest,testResultMap);
	}
	
	
	public Spider(PageProcessor pageProcessor,boolean isTest) {
		this.pageProcessor = pageProcessor;
		this.site = pageProcessor.getSite();
		this.startRequests = pageProcessor.getSite().getStartRequests();
		this.startTime = new Date();
		this.monitorId = IdUtils.id();
		this.isTest = isTest;
	}
	public Spider(PageProcessor pageProcessor,boolean isTest,Map<Long,List<String>> testResultMap) {
		this.pageProcessor = pageProcessor;
		this.site = pageProcessor.getSite();
		this.startRequests = pageProcessor.getSite().getStartRequests();
		this.startTime = new Date();
		this.monitorId = IdUtils.id();
		this.isTest = isTest;
		this.testResultMap = testResultMap;
	}
	


	/**
	 * Set startUrls of Spider.<br>
	 * Prior to startUrls of Site.
	 *
	 * @param startUrls
	 *            startUrls
	 * @return this
	 */
	public Spider startUrls(List<String> startUrls) {
		checkIfRunning();
		this.startRequests = UrlUtils.convertToRequests(startUrls);
		return this;
	}

	/**
	 * Set startUrls of Spider.<br>
	 * Prior to startUrls of Site.
	 *
	 * @param startRequests
	 *            startRequests
	 * @return this
	 */
	public Spider startRequest(List<Request> startRequests) {
		checkIfRunning();
		this.startRequests = startRequests;
		return this;
	}

	/**
	 * Set an uuid for spider.<br>
	 * Default uuid is domain of site.<br>
	 *
	 * @param uuid
	 *            uuid
	 * @return this
	 */
	public Spider setUUID(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * set scheduler for Spider
	 *
	 * @param scheduler
	 *            scheduler
	 * @return this
	 * @Deprecated
	 * @see #setScheduler(us.codecraft.webmagic.scheduler.Scheduler)
	 */
	public Spider scheduler(Scheduler scheduler) {
		return setScheduler(scheduler);
	}

	/**
	 * set scheduler for Spider
	 *
	 * @param scheduler
	 *            scheduler
	 * @return this
	 * @see Scheduler
	 * @since 0.2.1
	 */
	public Spider setScheduler(Scheduler scheduler) {
		checkIfRunning();
		Scheduler oldScheduler = this.scheduler;
		this.scheduler = scheduler;
		if (oldScheduler != null) {
			Request request;
			while ((request = oldScheduler.poll(this)) != null) {
				this.scheduler.push(request, this);
			}
		}
		return this;
	}

	/**
	 * add a pipeline for Spider
	 *
	 * @param pipeline
	 *            pipeline
	 * @return this
	 * @see #addPipeline(us.codecraft.webmagic.pipeline.Pipeline)
	 * @deprecated
	 */
	public Spider pipeline(Pipeline pipeline) {
		return addPipeline(pipeline);
	}

	/**
	 * add a pipeline for Spider
	 *
	 * @param pipeline
	 *            pipeline
	 * @return this
	 * @see Pipeline
	 * @since 0.2.1
	 */
	public Spider addPipeline(Pipeline pipeline) {
		checkIfRunning();
		this.pipelines.add(pipeline);
		return this;
	}

	/**
	 * set pipelines for Spider
	 *
	 * @param pipelines
	 *            pipelines
	 * @return this
	 * @see Pipeline
	 * @since 0.4.1
	 */
	public Spider setPipelines(List<Pipeline> pipelines) {
		checkIfRunning();
		this.pipelines = pipelines;
		return this;
	}

	/**
	 * clear the pipelines set
	 *
	 * @return this
	 */
	public Spider clearPipeline() {
		pipelines = new ArrayList<Pipeline>();
		return this;
	}

	/**
	 * set the downloader of spider
	 *
	 * @param downloader
	 *            downloader
	 * @return this
	 * @see #setDownloader(us.codecraft.webmagic.downloader.Downloader)
	 * @deprecated
	 */
	public Spider downloader(Downloader downloader) {
		return setDownloader(downloader);
	}

	/**
	 * set the downloader of spider
	 *
	 * @param downloader
	 *            downloader
	 * @return this
	 * @see Downloader
	 */
	public Spider setDownloader(Downloader downloader) {
		checkIfRunning();
		this.downloader = downloader;
		return this;
	}

	protected void initComponent() {
		if (downloader == null) {
			this.downloader = new HttpClientDownloader();
		}
		if (pipelines.isEmpty()) {
			pipelines.add(new ConsolePipeline());
		}
		downloader.setThread(threadNum);
		if (threadPool == null || threadPool.isShutdown()) {
			if (executorService != null && !executorService.isShutdown()) {
				threadPool = new CountableThreadPool(threadNum, executorService);
			} else {
				threadPool = new CountableThreadPool(threadNum);
			}
		}
		if (startRequests != null) {
			for (Request request : startRequests) {
				scheduler.push(request, this);
			}
			startRequests.clear();
		}
		// 开启监控
		if (spiderListeners != null) {
			for (SpiderListener listener : spiderListeners) {
				listener.onStart(startTime,regist,"",monitorId);
			}
		}

	}

	@Override
	public void run() {
		checkRunningStat();
		initComponent();
		logger.info("Spider " + getUUID() + " started!");
		while (!Thread.currentThread().isInterrupted() && stat.get() == STAT_RUNNING) {
			Request request = scheduler.poll(this);
			if (request == null) {
				if (threadPool.getThreadAlive() == 0 && exitWhenComplete) {
					break;
				}
				waitNewUrl();
			} else {
				final Request requestFinal = request;
				final Spider spiderFinal = this;
				requestFinal.putExtra("activeThreadNum", threadPool.getThreadAlive());
				requestFinal.putExtra("uuid", uuid);
				threadPool.execute(new Runnable() {
					@Override
					public void run() {
						try {
							processRequest(requestFinal);//处理任务
						} catch (Exception e) {
							
							//如果是沙箱测试
							if(isTest) {
								//传递异常信息
								if(testResultMap.get(site.getTopicId()) == null){
									testResultMap.put(site.getTopicId(), new ArrayList<String>());
								}
								testResultMap.get(site.getTopicId()).add(site.getLevel()+"级爬虫发生异常："+e.toString());
							}
							// 如果是淘宝的瞬时屏蔽异常，重试就行，不进行记录，这种异常很频繁。
							//--------------------------------------------框架里耦合的业务逻辑，不合适 TODO。
							if (!"block".equals(e.getMessage())) {
								e.printStackTrace();
								requestFinal.putExtra("exceptionMessage", e.getMessage());
								onError(requestFinal);
							}
							//-----------------------------------------
							// pageProcessor重试机制：重试次数加1，放回队列，由downloader判断是否要继续重试。
							Integer retryTimes = (Integer) requestFinal.getExtra(Request.CYCLE_TRIED_TIMES);
							retryTimes++;// 重试次数+1
							requestFinal.putExtra(Request.CYCLE_TRIED_TIMES, retryTimes);
							scheduler.push(requestFinal, spiderFinal); // 由scheduler push回数据库
							logger.info("任务出现异常：{}",e);
						} finally {
							if (site.getHttpProxyPool() != null && site.getHttpProxyPool().isEnable()) {
								site.returnHttpProxyToPool((HttpHost) requestFinal.getExtra(Request.PROXY),
										(Integer) requestFinal.getExtra(Request.STATUS_CODE), requestFinal.getUrl());
							}
							pageCount.incrementAndGet();
							signalNewUrl();
						}
					}
				});
			}
		}
		stat.set(STAT_STOPPED);
		// release some resources
		if (destroyWhenExit) {
			close();
		}
	}

	/**
	 * 爬虫的monitorId为宿主机器IP地址+爬虫开始时间；此方法有quartz框架实例化的爬虫调用，将自身放入QuartzSpiderRMIImpl类单实例
	 * 的map中。
	 * @param spider
	 */
	public void putIntoAutoSpiderMap(Spider spider) {
		try {
			regist = true;
			//String hostAddress = InetAddress.getLocalHost().getHostAddress();
			//获取单例，将该爬虫放入map数据结构中。
			QuartzSpiderRMIImpl quartzSpiders = QuartzSpiderRMIImpl.getInstance();
			//key就是monitorId
			//quartzSpiders.putSpider(hostAddress.replace(".", "") + sdf.format(spider.getStartTime()),spider);
			quartzSpiders.putSpider(monitorId,spider);
		} catch (RemoteException e) {
			logger.error("远程异常:{}", e);
		//} catch(UnknownHostException e){
			//logger.error("未知的主机:{}",e);
		}
	}

	protected void onError(Request request) {
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onError(request);
			}
		}
	}

	protected void onSuccess(Request request) {
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onSuccess(request);
			}
		}
	}

	private void checkRunningStat() {
		while (true) {
			int statNow = stat.get();
			if (statNow == STAT_RUNNING) {
				logger.warn("警告：爬虫的状态已经是running...");
			}
			if (stat.compareAndSet(statNow, STAT_RUNNING)) {
				break;
			}
		}
	}

	public void close() {
		// 关闭前先将信息打印出来，用以后续分析
		if (null != site && null != site.getHttpProxyPool()) {
			logger.info(site.getHttpProxyPool().allProxyStatus()); // FIXME
																	// 写入到特定的文件，用于统计
		}
		// 调用listener钩子方法
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onClose();
			}
		}

		destroyEach(downloader);
		destroyEach(pageProcessor);
		destroyEach(scheduler);

		for (Pipeline pipeline : pipelines) {
			destroyEach(pipeline);
		}

		
		
		
		threadPool.shutdown();
		logger.info("close down spider ... the pageCount is " + pageCount.get());
		logger.info("the start time is " + startTime);
		logger.info("the finish time is " + new Date());
	}

	private void destroyEach(Object object) {
		if (object instanceof Closeable) {
			try {
				((Closeable) object).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Process specific urls without url discovering.
	 *
	 * @param urls
	 *            urls to process
	 */
	public void test(String... urls) {
		initComponent();
		if (urls.length > 0) {
			for (String url : urls) {
				try {
					processRequest(new Request(url));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void processRequest(Request request) throws Exception {
		// downloader处理
		Page page = null;
		if (spiderListeners != null && spiderListeners.size() > 0) {
			Date timeBeforeDownload = new Date();
			page = downloader.download(request, this);
			Long downloaderSpendTime = System.currentTimeMillis() - timeBeforeDownload.getTime();
			request.putExtra("downloaderTime", downloaderSpendTime);
		} else {
			page = downloader.download(request, this);
		}
		if (page == null) {// 超过重试次数，放弃任务。----重试机制
			sleep(site.getRetrySleepTime());
			try{
				onFail(request);// 把废弃url写到mongodb。
			}catch(Exception e){
				logger.error("监听器钩子方法onFail调用时出错。");
			}
			return;
		}
		//如果downloader出错，需要重试，提取任务url加入mongo，直接结束方法。downloader----重试机制。
		/*if (page.isNeedCycleRetry()) {
			extractAndAddRequests(page, true);
			sleep(site.getRetrySleepTime());
			return;
		}*/
		// pageProcessor处理,加入自定义脚本执行功能
		if(site.getCookies().containsKey("user-define")&& site.getCookies().containsValue("user-define")){
			if (spiderListeners != null && spiderListeners.size() > 0) {
				Date timeBeforePageProcessor = new Date();
				GroovyCommonUtil.invokeMethod(site.getPageProcessorScript(),"process", page);
				Long pageProcessorSpendTime = System.currentTimeMillis() - timeBeforePageProcessor.getTime();
				request.putExtra("pageProcessorTime", pageProcessorSpendTime);
			} else {
				GroovyCommonUtil.invokeMethod(site.getPageProcessorScript(),"process", page);
			}
		}else{
			if (spiderListeners != null && spiderListeners.size() > 0) {
				Date timeBeforePageProcessor = new Date();
				pageProcessor.process(page);
				Long pageProcessorSpendTime = System.currentTimeMillis() - timeBeforePageProcessor.getTime();
				request.putExtra("pageProcessorTime", pageProcessorSpendTime);
			} else {
				pageProcessor.process(page);
			}
		}
		
		extractAndAddRequests(page, spawnUrl);//提取pageProcessor解析过程中添加的url，并加入mongo。
		
		if (!page.getResultItems().isSkip()) {
			if (spiderListeners != null && spiderListeners.size() > 0) {
				Date timeBeforePipeline = new Date();
				for (Pipeline pipeline : pipelines) {
					pipeline.process(page.getResultItems(), this);
				}
				Long pipelineSpendTime = System.currentTimeMillis() - timeBeforePipeline.getTime();
				request.putExtra("pipelineTime", pipelineSpendTime);
			} else {
				for (Pipeline pipeline : pipelines) {
					pipeline.process(page.getResultItems(), this);
				}
			}
		}
		// for proxy status management
		request.putExtra(Request.STATUS_CODE, page.getStatusCode());
		try{
			onSuccess(request);// 成功钩子，钩子方法不参与爬虫异常重试，异常自己处理。
		} catch(Exception e) {
			logger.error("监听器onSuccess方法调用时出错。");
		}
		sleep(site.getSleepTime());
	}

	/**
	 * 将失败次数过多的request写入mongoDB
	 * 
	 * @param request
	 */
	private void onFail(Request request) {
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onFail(request);
			}
		}
	}

	protected void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void extractAndAddRequests(Page page, boolean spawnUrl) {
		if (spawnUrl && CollectionUtils.isNotEmpty(page.getTargetRequests())) {
			for (Request request : page.getTargetRequests()) {
				addRequest(request);
			}
		}
	}

	private void addRequest(Request request) {
		if (site.getDomain() == null && request != null && request.getUrl() != null) {
			site.setDomain(UrlUtils.getDomain(request.getUrl()));
		}
		scheduler.push(request, this);//push 回去
	}

	protected void checkIfRunning() {
		if (stat.get() == STAT_RUNNING) {
			throw new IllegalStateException("Spider is already running!");
		}
	}

	public void runAsync() {
		Thread thread = new Thread(this);
		thread.setDaemon(false);
		thread.start();
	}

	/**
	 * Add urls to crawl. <br>
	 *
	 * @param urls
	 *            urls
	 * @return this
	 */
	public Spider addUrl(String... urls) {
		for (String url : urls) {
			addRequest(new Request(url));
		}
		signalNewUrl();
		return this;
	}

	/**
	 * Download urls synchronizing.
	 *
	 * @param urls
	 *            urls
	 * @return list downloaded
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> getAll(Collection<String> urls) {
		destroyWhenExit = false;
		spawnUrl = false;
		startRequests.clear();
		for (Request request : UrlUtils.convertToRequests(urls)) {
			addRequest(request);
		}
		CollectorPipeline collectorPipeline = getCollectorPipeline();
		pipelines.add(collectorPipeline);
		run();
		spawnUrl = true;
		destroyWhenExit = true;
		return collectorPipeline.getCollected();
	}

	@SuppressWarnings("rawtypes")
	protected CollectorPipeline getCollectorPipeline() {
		return new ResultItemsCollectorPipeline();
	}

	public <T> T get(String url) {
		List<String> urls = Lists.newArrayList(url);
		List<T> resultItemses = getAll(urls);
		if (resultItemses != null && resultItemses.size() > 0) {
			return resultItemses.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Add urls with information to crawl.<br>
	 *
	 * @param requests
	 *            requests
	 * @return this
	 */
	public Spider addRequest(Request... requests) {
		for (Request request : requests) {
			if(request.getUrl()!=null)
			addRequest(request);
		}
		signalNewUrl();
		return this;
	}

	private void waitNewUrl() {
		newUrlLock.lock();
		try {
			// double check
			if (threadPool.getThreadAlive() == 0 && exitWhenComplete) {
				return;
			}
			newUrlCondition.await(emptySleepTime, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.warn("waitNewUrl - interrupted, error {}", e);
		} finally {
			newUrlLock.unlock();
		}
	}

	private void signalNewUrl() {
		try {
			newUrlLock.lock();
			newUrlCondition.signalAll();
		} finally {
			newUrlLock.unlock();
		}
	}

	public void start() {
		runAsync();
	}

	public void stop() {
		if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
			logger.info("Spider " + getUUID() + " stop success!");
		} else {
			logger.info("Spider " + getUUID() + " stop fail!");
		}
	}

	/**
	 * start with more than one threads
	 *
	 * @param threadNum
	 *            threadNum
	 * @return this
	 */
	public Spider thread(int threadNum) {
		checkIfRunning();
		this.threadNum = threadNum;
		if (threadNum <= 0) {
			throw new IllegalArgumentException("threadNum should be more than one!");
		}
		return this;
	}

	/**
	 * start with more than one threads
	 *
	 * @param executorService
	 *            executorService to run the spider
	 * @param threadNum
	 *            threadNum
	 * @return this
	 */
	public Spider thread(ExecutorService executorService, int threadNum) {
		checkIfRunning();
		this.threadNum = threadNum;
		if (threadNum <= 0) {
			throw new IllegalArgumentException("threadNum should be more than one!");
		}
		return this;
	}

	public boolean isExitWhenComplete() {
		return exitWhenComplete;
	}

	/**
	 * Exit when complete. <br>
	 * True: exit when all url of the site is downloaded. <br>
	 * False: not exit until call stop() manually.<br>
	 *
	 * @param exitWhenComplete
	 *            exitWhenComplete
	 * @return this
	 */
	public Spider setExitWhenComplete(boolean exitWhenComplete) {
		this.exitWhenComplete = exitWhenComplete;
		return this;
	}

	public boolean isSpawnUrl() {
		return spawnUrl;
	}

	/**
	 * Get page count downloaded by spider.
	 *
	 * @return total downloaded page count
	 * @since 0.4.1
	 */
	public long getPageCount() {
		return pageCount.get();
	}

	/**
	 * Get running status by spider.
	 *
	 * @return running status
	 * @see Status
	 * @since 0.4.1
	 */
	public Status getStatus() {
		return Status.fromValue(stat.get());
	}

	public enum Status {
		Init(0), Running(1), Stopped(2);

		private Status(int value) {
			this.value = value;
		}

		private int value;

		int getValue() {
			return value;
		}

		public static Status fromValue(int value) {
			for (Status status : Status.values()) {
				if (status.getValue() == value) {
					return status;
				}
			}
			// default value
			return Init;
		}
	}

	/**
	 * Get thread count which is running
	 *
	 * @return thread count which is running
	 * @since 0.4.1
	 */
	public int getThreadAlive() {
		if (threadPool == null) {
			return 0;
		}
		return threadPool.getThreadAlive();
	}

	/**
	 * Whether add urls extracted to download.<br>
	 * Add urls to download when it is true, and just download seed urls when it
	 * is false. <br>
	 * DO NOT set it unless you know what it means!
	 *
	 * @param spawnUrl
	 *            spawnUrl
	 * @return this
	 * @since 0.4.0
	 */
	public Spider setSpawnUrl(boolean spawnUrl) {
		this.spawnUrl = spawnUrl;
		return this;
	}

	@Override
	public String getUUID() {
		if (uuid != null) {
			return uuid;
		}
		uuid = UUID.randomUUID().toString();
		return uuid;
		// if (site != null) {
		// return site.getDomain();
		// }
		//

	}

	public Spider setExecutorService(ExecutorService executorService) {
		checkIfRunning();
		this.executorService = executorService;
		return this;
	}

	@Override
	public Site getSite() {
		return site;
	}

	public List<SpiderListener> getSpiderListeners() {
		return spiderListeners;
	}
	
	/**
	 * 设置listener，需要searchId？优化 TODO
	 * @param spiderListeners
	 * @return
	 */
	public Spider setSpiderListeners(List<SpiderListener> spiderListeners) {
		this.spiderListeners = spiderListeners;
		return this;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Set wait time when no url is polled.<br>
	 * <br>
	 *
	 * @param emptySleepTime
	 *            In MILLISECONDS.
	 */
	public void setEmptySleepTime(int emptySleepTime) {
		this.emptySleepTime = emptySleepTime;
	}

	public AtomicInteger getStat() {
		return stat;
	}

	public void setStat(AtomicInteger stat) {
		this.stat = stat;
	}

	public PageProcessor getPageProcessor() {
		return pageProcessor;
	}
	
	
	
	
	
}
