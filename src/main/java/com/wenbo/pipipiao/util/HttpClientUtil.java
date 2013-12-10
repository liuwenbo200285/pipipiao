package com.wenbo.pipipiao.util;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import com.wenbo.pipipiao.enumutil.TrainSeatEnum;
import com.wenbo.pipipiao.enumutil.UrlEnum;
import com.wenbo.pipipiao.enumutil.UrlNewEnum;

/**
 * httpClient工具类
 * @author Administrator
 *
 */
public class HttpClientUtil {
	
	public static final String REFER = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
	/**
	 *requestType 1 GET 2 POST
	 */
	public static HttpPost getHttpPost(URI uri,UrlEnum urlEnum){
		HttpPost httpPost = new HttpPost(uri);
		if(StringUtils.isNotEmpty(urlEnum.getAccept())){
			httpPost.addHeader("Accept",urlEnum.getAccept());
		}
		httpPost.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpPost.addHeader("Cache-Control","max-age=0");
		httpPost.addHeader("Connection","keep-alive");
		httpPost.addHeader("Origin","https://dynamic.12306.cn");
		httpPost.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		httpPost.addHeader("Host","dynamic.12306.cn");
		httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17");
		if(StringUtils.isNotEmpty(urlEnum.getxRequestWith())){
			httpPost.addHeader("X-Requested-With",urlEnum.getxRequestWith());
		}
		if(StringUtils.isNotEmpty(urlEnum.getContentType())){
			httpPost.addHeader("Content-Type",urlEnum.getContentType());
		}
		if(StringUtils.isNotEmpty(urlEnum.getRefer())){
			httpPost.addHeader("Referer",urlEnum.getRefer());
		}
		return httpPost;
	}
	
	public static HttpPost getNewHttpPost(UrlNewEnum urlEnum){
		HttpPost httpPost = new HttpPost(UrlNewEnum.DO_MAIN.getPath()+urlEnum.getPath());
		if(StringUtils.isNotEmpty(urlEnum.getAccept())){
			httpPost.addHeader("Accept",urlEnum.getAccept());
		}
		httpPost.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpPost.addHeader("Cache-Control","max-age=0");
		httpPost.addHeader("Connection","keep-alive");
		httpPost.addHeader("Origin","https://kyfw.12306.cn");
		httpPost.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		httpPost.addHeader("Host","kyfw.12306.cn");
		httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17");
		if(StringUtils.isNotEmpty(urlEnum.getxRequestWith())){
			httpPost.addHeader("X-Requested-With",urlEnum.getxRequestWith());
		}
		if(StringUtils.isNotEmpty(urlEnum.getContentType())){
			httpPost.addHeader("Content-Type",urlEnum.getContentType());
		}
		if(StringUtils.isNotEmpty(urlEnum.getRefer())){
			httpPost.addHeader("Referer",urlEnum.getRefer());
		}
		return httpPost;
	}
	
	public static HttpGet getNewHttpGet(URI uri,UrlNewEnum urlEnum){
		HttpGet httpGet = new HttpGet(uri);
		if(StringUtils.isNotEmpty(urlEnum.getAccept())){
			httpGet.addHeader("Accept",urlEnum.getAccept());
		}
		httpGet.addHeader("Cache-Control","max-age=0");
		httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpGet.addHeader("Connection","keep-alive");
		httpGet.addHeader("Origin","https://kyfw.12306.cn");
		httpGet.addHeader("Host","kyfw.12306.cn");
		httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
		if(StringUtils.isNotEmpty(urlEnum.getxRequestWith())){
			httpGet.addHeader("X-Requested-With",urlEnum.getxRequestWith());
		}
		if(StringUtils.isNotEmpty(urlEnum.getContentType())){
			httpGet.addHeader("Content-Type",urlEnum.getContentType());
		}
		if(StringUtils.isNotEmpty(urlEnum.getRefer())){
			httpGet.addHeader("Referer",urlEnum.getRefer());
		}
		return httpGet;
	}
	
	public static HttpGet getHttpGet(URI uri,UrlEnum urlEnum){
		HttpGet httpGet = new HttpGet(uri);
		if(StringUtils.isNotEmpty(urlEnum.getAccept())){
			httpGet.addHeader("Accept",urlEnum.getAccept());
		}
		httpGet.addHeader("Cache-Control","max-age=0");
		httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpGet.addHeader("Connection","keep-alive");
		httpGet.addHeader("Host","dynamic.12306.cn");
		httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
		if(StringUtils.isNotEmpty(urlEnum.getxRequestWith())){
			httpGet.addHeader("X-Requested-With",urlEnum.getxRequestWith());
		}
		if(StringUtils.isNotEmpty(urlEnum.getContentType())){
			httpGet.addHeader("Content-Type",urlEnum.getContentType());
		}
		if(StringUtils.isNotEmpty(urlEnum.getRefer())){
			httpGet.addHeader("Referer",urlEnum.getRefer());
		}
		return httpGet;
	}
	
	/**
	 * 获取坐席枚举
	 * @param trainSeat
	 * @return
	 */
	public static TrainSeatEnum getSeatEnum(Integer trainSeat){
		TrainSeatEnum[] trainSeatEnums = TrainSeatEnum.values();
		for(TrainSeatEnum trainSeatEnum:trainSeatEnums){
			if(trainSeatEnum.getCode() == trainSeat){
				return trainSeatEnum;
			}
		}
		return null;
	}
}
