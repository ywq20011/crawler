package com.hlg.webgleaner.core.processor;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * 指定路径的页面处理器
 * @author linjx
 * @Date 2016年3月8日
 * @Version 1.0.0
 */
public class SpecifyPathPageProcessor implements PageProcessor {

    private Site site;

    public SpecifyPathPageProcessor(String startUrl, String urlPattern) {

    }

    @Override
    public void process(Page page) {
    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
