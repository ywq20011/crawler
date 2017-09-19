package com.hlg.webgleaner.core.listener.bo;

/**
 * 爬虫类型
 * 
 * @author yangwq
 */
public enum SpiderType {
	JD_Item_List, // 爬取京东商品列表基本信息
	JD_Item_Info, // 爬取京东商品详情信息
	TaoBao_TMALL_Shop, // 爬取淘宝天猫商店信息
	TaoBao_TMALL_Item_List, // 爬取淘宝和天猫店铺商品基本信息
	TaoBao_Item_Info, // 爬取淘宝商品详情信息
	TMALL_Item_Info, // 爬取天猫商品详情
	others;// 其他
}