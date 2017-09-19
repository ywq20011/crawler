package com.hlg.webgleaner.core.downloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.Html;

/**
 * Base class of downloader with some common methods.
 *
 * @author code4crafter@gmail.com
 * @since 0.5.0
 * @update linjx@linesum.com
 */
public abstract class AbstractDownloader implements Downloader {

	private static final String DEFAULT_CHARSET = "utf-8";

	/**
	 * A simple method to download a url.
	 *
	 * @param url
	 *            url
	 * @return html
	 * @throws Exception 
	 */
	public Html download(String url) throws Exception {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		return download(url, DEFAULT_CHARSET);
	}

	/**
	 * A simple method to download a url.
	 *
	 * @param url
	 *            url
	 * @param charset
	 *            charset
	 * @return html
	 */
	public Html download(String url, String charset) throws Exception {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		// Note：这里做了修改，增加了setDomain(url)
		Page page = download(new Request(url), Site.me().setCharset(charset).setDomain(getDomainName(url)).toTask());
		return (Html) page.getHtml();
	}

	/**
	 * 钩子方法，成功
	 * 
	 * @param request
	 */
	protected void onSuccess(Request request) {
	}

	/**
	 * 钩子方法，失败
	 * 
	 * @param request
	 */
	protected void onError(Request request) {
	}

	protected Page addToCycleRetry(Request request) {
		Page page = new Page();
		Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
		int cycleTriedTimes = (Integer) cycleTriedTimesObject;
		cycleTriedTimes++;
		page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
		page.setNeedCycleRetry(true);//设置为true，打上重试标志。
		return page;
	}

	private String getDomainName(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
		Matcher m = p.matcher(url);
		if (m.find()) {
			return m.group();
		} else {
			return url;
		}
	}
}
