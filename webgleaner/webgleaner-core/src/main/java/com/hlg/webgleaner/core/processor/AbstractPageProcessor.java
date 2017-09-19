package com.hlg.webgleaner.core.processor;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.proxy.Proxy;

public class AbstractPageProcessor implements PageProcessor {

	public void process(Page page) throws Exception {
		
	}

	public Site getSite() {
		return Site.me();
	}
	
	/**
	 * 被屏蔽后在PageProcessor重新添加回scheduler<br>
	 * 是否是被屏蔽而重定向，需要开发自己判断
	 * @param request
	 * @param site
	 * @return
	 */
	protected Page bannedToCycleRetry(Page page, Site site) {
		Request request = page.getRequest();
        Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
        request.putExtra(Request.STATUS_CODE, Integer.valueOf(Proxy.ERROR_BANNED));  //改变状态码
        if (cycleTriedTimesObject == null) {
            page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
        } else {
            int cycleTriedTimes = (Integer) cycleTriedTimesObject;
            cycleTriedTimes++;
            if (cycleTriedTimes >= site.getCycleRetryTimes()) {
                return null;
            }
            page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
        }
        page.setNeedCycleRetry(true);
        return page;
    }

}
