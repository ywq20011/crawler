package us.codecraft.webmagic;

import java.util.Date;

/**
 * Listener of Spider on page processing. Used for monitor and such on.
 *
 * @author code4crafer@gmail.com
 * @since 0.5.0
 */
public interface SpiderListener {

	/**
	 * 任务爬取成功时调用。
	 * @param request
	 */
    public void onSuccess(Request request);

    /**
     * 任务爬取失败时调用
     * @param request
     */
    public void onError(Request request);

    /**
     * 任务爬取失败超过重试次数时调用
     * @param request
     */
	public void onFail(Request request);
	
	public void onStart(Date date,boolean regist,String searchId ,Long monitorId);

	/**
	 * 爬虫结束时调用
	 */
	public void onClose();
}
