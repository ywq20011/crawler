package com.hlg.webgleaner.core.processor;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * 简单的页面处理器，只提取HTML页面，不做解析
 * @author linjx
 * @Date 2016年3月8日
 * @Version 1.0.0
 */
public class ExtractHtmlPageProcessor implements PageProcessor {

    private Site site;

    @SuppressWarnings({ "deprecation", "static-access" })
	public ExtractHtmlPageProcessor(String startUrl) {
    	this.site = site.me().addStartUrl(startUrl).setDomain(UrlUtils.getDomain(startUrl));
    }

    @Override
    public void process(Page page) {
//    	List<String> requests = page.getHtml().links().regex(//regex)
//    	page.putField("itemUrls", page.getHtml().links().regex(".*//detail.tmall.com/item.htm?id=.*").all());
    	System.out.println(page.getHtml().css("dl.col").all());
    	page.putField("html", page.getHtml().toString());
    	/*String json = page.getHtml().regex("g_page_config.*").regex(".*}};", 1).toString();
    	json = StringUtils.substring(json, "g_page_config = ".length(), json.length()-1);
    	System.out.println(json);
    	JSONObject jobj = JSON.parseObject(json);
    	JSONObject mods = jobj.getJSONObject("mods");
    	JSONArray shopItems = mods.getJSONObject("shoplist").getJSONObject("data").getJSONArray("shopItems");
    	page.putField("pageJson", shopItems);*/
    	page.putField("charset", page.getResultItems().get("charset"));
    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
