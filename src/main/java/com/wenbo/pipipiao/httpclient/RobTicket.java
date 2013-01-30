package com.wenbo.pipipiao.httpclient;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wenbo.pipipiao.domain.ConfigInfo;
import com.wenbo.pipipiao.domain.UserInfo;
import com.wenbo.pipipiao.util.HttpClientUtil;

/**
 * 抢火车票主程序
 * @author Administrator
 *
 */
public class RobTicket {
	
	private ConfigInfo configInfo;
	
	private UserInfo userInfo;
	
	private Map<String,UserInfo> userInfoMap = null;
	
	private HttpClient httpClient = null;
	
	public RobTicket(ConfigInfo configInfo,UserInfo userInfo,Map<String,
			UserInfo> userInfoMap,HttpClient httpClient){
		this.configInfo = configInfo;
		this.userInfo = userInfo;
		this.userInfoMap = userInfoMap;
		this.httpClient = httpClient;
	}
	
	/**
	 * 获取登录码
	 */
	public void getLoginRand(){
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.LOGIN_INIT_URL.getPath())
			    .setParameter("method","loginAysnSuggest");
			URI uri = builder.build();
			HttpGet httpget = HttpClientUtil.getHttpGet(uri, UrlEnum.LOGIN_INIT_URL);
			response = httpClient.execute(httpget);
			HttpEntity entity = response.getEntity();
			 if (entity != null && entity.getContentLength() > 0) {
			     InputStream instream = entity.getContent();
			     BufferedReader reader = null;
			     try {
			    	 reader = new BufferedReader(
			                 new InputStreamReader(instream));
			         String str = reader.readLine();
			         JSONObject object = JSONObject.parseObject(str);
			         login(object.getString("loginRand"),object.getString("randError"));
			     } catch (IOException ex) {
			         throw ex;

			     } catch (RuntimeException ex) {
			         httpget.abort();
			         throw ex;
			     } finally {
			    	 HttpClientUtils.closeQuietly(response);
			         IOUtils.closeQuietly(reader);
			     }
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void login(String loginRand,String randError) throws ClientProtocolException, IOException{
		HttpResponse response = null;
		//获取验证码
		try {
			String randCode = getRandCode(UrlEnum.LOGIN_RANGCODE_URL);
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","login"));
			parameters.add(new BasicNameValuePair("loginRand",loginRand));
			parameters.add(new BasicNameValuePair("refundLogin","N"));
			parameters.add(new BasicNameValuePair("refundFlag", "Y"));
			parameters.add(new BasicNameValuePair("loginUser.user_name",configInfo.getUsername()));
			parameters.add(new BasicNameValuePair("nameErrorFocus",""));
			parameters.add(new BasicNameValuePair("user.password",configInfo.getUserpass()));
			parameters.add(new BasicNameValuePair("randCode",randCode));
			parameters.add(new BasicNameValuePair("randErrorFocus",""));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.LONGIN_CONFIM.getPath());
			URI uri = builder.build();
			HttpPost httpPost = HttpClientUtil.getHttpPost(uri,UrlEnum.LONGIN_CONFIM);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
			}else if(response.getStatusLine().getStatusCode() == 404){
			}else if(response.getStatusLine().getStatusCode() == 200){
				Document document = JsoupUtil.getPageDocument(response.getEntity().getContent());
				//判断登录状态
				if(JsoupUtil.validateLogin(document)){
					if(userInfoMap == null){
						getOrderPerson();
					}
					searchTicket("2013-02-10");
				}else{
					getLoginRand();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	/**
	 * 获取登录账号用户信息
	 * @throws URISyntaxException 
	 */
	public void getOrderPerson() throws URISyntaxException{
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.GET_ORDER_PERSON.getPath())
			    .setParameter("method","getpassengerJson");
			URI uri = builder.build();
			HttpPost httpPost = HttpClientUtil.getHttpPost(uri,UrlEnum.GET_ORDER_PERSON);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 200){
				String info = EntityUtils.toString(response.getEntity());
				JSONObject jsonObject = JSON.parseObject(info);
				List<UserInfo> userInfos =  JSONArray.parseArray(jsonObject.getString("passengerJson"),UserInfo.class);
				if(userInfos == null || userInfos.size() == 0){
					System.out.println("此账号没有添加联系人!");
					return;
				}
				userInfoMap = new HashMap<String, UserInfo>();
				UserInfo userInfo = null;
				for(int i = 0; i < userInfos.size(); i++){
					userInfo = userInfos.get(i);
					if(userInfo != null){
						userInfo.setIndex(i);
						userInfoMap.put(userInfo.getPassenger_name(), userInfo);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	
	/***
	 * 构造查询火车票对象
	 * @throws URISyntaxException 
	 **/
	public void searchTicket(String date){
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/querySingleAction.do")
			    .setParameter("method","queryLeftTicket")
			    .setParameter("orderRequest.train_date",date)
			    .setParameter("orderRequest.from_station_telecode", "CWQ")
			    .setParameter("orderRequest.to_station_telecode", "SZQ")
			    .setParameter("orderRequest.train_no", "6a000K907507")
			    .setParameter("trainPassType","QB")
			    .setParameter("trainClass", "K#")
			    .setParameter("includeStudent","00")
			    .setParameter("seatTypeAndNum","")
			    .setParameter("orderRequest.start_time_str","00:00--24:00");
			URI uri = builder.build();
			HttpGet httpGet = HttpClientUtil.getHttpGet(uri,UrlEnum.SEARCH_TICKET);
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				checkTickeAndOrder(response,date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	/**
	 * 检测出票结果以及刷票
	 * @param inputStream
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public void checkTickeAndOrder(HttpResponse response,String date) throws IllegalStateException, IOException{
		Document document = null;
		try {
			document = JsoupUtil.getPageDocument(response.getEntity().getContent());
			if(JsoupUtil.checkHaveTicket(document,7)){
				System.out.println("有票了，开始订票~~~~~~~~~");
				String[] params = JsoupUtil.getTicketInfo(document);
				orderTicket(date,params);
			}
			else{
				System.out.println("休息二秒，继续刷票");
				Thread.sleep(2000);
				searchTicket(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	/**
	 * 点击预定
	 * @param date
	 * @param params
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public  void orderTicket(String date,String[] params) throws IllegalStateException, IOException{
		HttpPost httpPost = null;
		OutputStream outputStream = null;
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","submutOrderRequest"));
			parameters.add(new BasicNameValuePair("arrive_time",params[6]));
			parameters.add(new BasicNameValuePair("from_station_name",params[7]));
			parameters.add(new BasicNameValuePair("from_station_no", params[9]));
			parameters.add(new BasicNameValuePair("from_station_telecode", params[4]));
			parameters.add(new BasicNameValuePair("from_station_telecode_name",params[7]));
			parameters.add(new BasicNameValuePair("include_student","00"));
			parameters.add(new BasicNameValuePair("lishi",params[1]));
			parameters.add(new BasicNameValuePair("locationCode", params[13]));
			parameters.add(new BasicNameValuePair("mmStr",params[12]));
			parameters.add(new BasicNameValuePair("round_start_time_str","00:00--24:00"));
			parameters.add(new BasicNameValuePair("round_train_date", date));
			parameters.add(new BasicNameValuePair("seattype_num",""));
			parameters.add(new BasicNameValuePair("single_round_type","1"));
			parameters.add(new BasicNameValuePair("start_time_str", "00:00--24:00"));
			parameters.add(new BasicNameValuePair("station_train_code", params[0]));
			parameters.add(new BasicNameValuePair("to_station_name", params[8]));
			parameters.add(new BasicNameValuePair("to_station_no", params[10]));
			parameters.add(new BasicNameValuePair("to_station_telecode", params[5]));
			parameters.add(new BasicNameValuePair("to_station_telecode_name",params[8]));
			parameters.add(new BasicNameValuePair("train_class_arr","D#"));
			parameters.add(new BasicNameValuePair("train_date", date));
			parameters.add(new BasicNameValuePair("train_pass_type","QB"));
			parameters.add(new BasicNameValuePair("train_start_time", params[2]));
			parameters.add(new BasicNameValuePair("trainno4", params[3]));
			parameters.add(new BasicNameValuePair("ypInfoDetail", params[11]));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.BOOK_TICKET.getPath());
			URI uri = builder.build();
			httpPost = HttpClientUtil.getHttpPost(uri,UrlEnum.BOOK_TICKET);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
				Header locationHeader = response.getFirstHeader("Location");
				System.out.println(locationHeader.getValue());
			}else if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity httpEntity = response.getEntity();
				Document document = JsoupUtil.getPageDocument(httpEntity.getContent());
				String ticketNo = null;
				String seatNum = null;
				String token = null;
				Element element = document.getElementById("left_ticket");
				if(element != null){
					ticketNo = element.attr("value");
					System.out.println(ticketNo);
				}else{
					System.out.println("~~~~~~~~~~~~~~~~~~~~~可能还有未处理的订单~~~~~~~~~~~~~~~~!");
					return;
				}
				element = document.getElementById("passenger_1_seat");
				if(element != null){
					Elements elements = element.getElementsContainingOwnText("硬卧");
					seatNum = elements.get(0).attr("value");
					System.out.println(seatNum);
				}
				Elements elements = document.getElementsByAttributeValue("name","org.apache.struts.taglib.html.TOKEN");
				if(elements != null){
					token = elements.get(0).attr("value");
				}
				checkOrderInfo(ticketNo,seatNum,token,params,date);
			}else{
				System.out.println(EntityUtils.toString(response.getEntity()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
			IOUtils.closeQuietly(outputStream);
		}
	}
	
	/**
	 * 检测票
	 * @param ticketNo
	 * @param seatNum
	 * @param token
	 * @param params
	 * @param date
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public  void checkOrderInfo(String ticketNo,String seatNum,String token,String[] params,String date){
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","checkOrderInfo"));
			parameters.add(new BasicNameValuePair("org.apache.struts.taglib.html.TOKEN",token));
			parameters.add(new BasicNameValuePair("leftTicketStr",ticketNo));
			parameters.add(new BasicNameValuePair("textfield","中文或拼音首字母"));
			//一个人只有一个checkbox0
			parameters.add(new BasicNameValuePair("checkbox0","0"));
			parameters.add(new BasicNameValuePair("checkbox2","2"));
			parameters.add(new BasicNameValuePair("orderRequest.train_date",date));
			parameters.add(new BasicNameValuePair("orderRequest.train_no", params[3]));
			parameters.add(new BasicNameValuePair("orderRequest.station_train_code", params[0]));
			parameters.add(new BasicNameValuePair("orderRequest.from_station_telecode", params[4]));
			parameters.add(new BasicNameValuePair("orderRequest.to_station_telecode", params[5]));
			parameters.add(new BasicNameValuePair("orderRequest.seat_type_code",""));
			parameters.add(new BasicNameValuePair("orderRequest.ticket_type_order_num",""));
			
			parameters.add(new BasicNameValuePair("orderRequest.bed_level_order_num","000000000000000000000000000000"));
			parameters.add(new BasicNameValuePair("orderRequest.start_time",params[2]));
			parameters.add(new BasicNameValuePair("orderRequest.end_time",params[6]));
			parameters.add(new BasicNameValuePair("orderRequest.from_station_name",params[7]));
			parameters.add(new BasicNameValuePair("orderRequest.to_station_name",params[8]));
			parameters.add(new BasicNameValuePair("orderRequest.cancel_flag","1"));
			parameters.add(new BasicNameValuePair("orderRequest.id_mode","Y"));
			
			//订票人信息 第一个人
			parameters.add(new BasicNameValuePair("passengerTickets","3,0,1,刘文波,1,430981198702272830,,Y"));
			parameters.add(new BasicNameValuePair("oldPassengers","刘文波,1,430981198702272830"));
			parameters.add(new BasicNameValuePair("passenger_1_seat","3"));
			parameters.add(new BasicNameValuePair("passenger_1_ticket","1"));
			parameters.add(new BasicNameValuePair("passenger_1_name","刘文波"));
			parameters.add(new BasicNameValuePair("passenger_1_cardtype","1"));
			parameters.add(new BasicNameValuePair("passenger_1_cardno","430981198702272830"));
			parameters.add(new BasicNameValuePair("passenger_1_mobileno",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));

			parameters.add(new BasicNameValuePair("passengerTickets","3,0,1,刘丽,1,430181198406030024,18606521059,Y"));
			parameters.add(new BasicNameValuePair("oldPassengers","刘丽,1,430181198406030024"));
			parameters.add(new BasicNameValuePair("passenger_2_seat","3"));
			parameters.add(new BasicNameValuePair("passenger_2_ticket","1"));
			parameters.add(new BasicNameValuePair("passenger_2_name","刘丽"));
			parameters.add(new BasicNameValuePair("passenger_2_cardtype","1"));
			parameters.add(new BasicNameValuePair("passenger_2_cardno","430181198406030024"));
			parameters.add(new BasicNameValuePair("passenger_2_mobileno","18606521059"));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("orderRequest.reserve_flag","A"));
			parameters.add(new BasicNameValuePair("tFlag","dc"));
			String rangCode = getRandCode(UrlEnum.ORDER_RANGCODE_URL);
			parameters.add(new BasicNameValuePair("rand",rangCode));
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.GET_ORDER_INFO.getPath());
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URI uri = builder.build();
			HttpPost httpPost = HttpClientUtil.getHttpPost(uri,UrlEnum.GET_ORDER_INFO);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 200){
				JSONObject jsonObject = JSON.parseObject(EntityUtils.toString(response.getEntity()));
				if(jsonObject != null
						&& "Y".equals(jsonObject.getString("errMsg"))){
					String msg = jsonObject.getString("msg");
					if(StringUtils.isNotEmpty(msg)){
						System.out.println(msg);
					}else{
						checkTicket(ticketNo,seatNum,token,params,date,rangCode);
					}
				}else{
					System.out.println(jsonObject.getString("errMsg"));
					checkOrderInfo(ticketNo,seatNum,token,params,date);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	/**
	 * 检测有没有票
	 * @param ticketNo
	 * @param params
	 */
	public  void checkTicket(String ticketNo,String seatNum,String token,String[] params,String date,String rangCode){
		HttpResponse response = null;
		try {
			HttpGet httpGet = new HttpGet("https://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=getQueueCount&train_date=2013-02-10&train_no=6a000K907507&station=K9075&seat=3&from=CSQ&to=BJQ&ticket=1014203020404410002410142001683028200072");
			httpGet.addHeader("Accept","application/json, text/javascript, */*");
			httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
			httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8");
			httpGet.addHeader("Connection","keep-alive");
			httpGet.addHeader("Content-Type","application/x-www-form-urlencoded");
			httpGet.addHeader("X-Requested-With","XMLHttpRequest");
			httpGet.addHeader("Host","dynamic.12306.cn");
			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17");
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				orderTicketToQueue(ticketNo,seatNum,token,params,date,rangCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	public void orderTicketToQueue(String ticketNo,String seatNum,String token,String[] params,String date,String rangCode){
		HttpResponse response = null;
		try {
			URIBuilder builder = new URIBuilder();
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","confirmSingleForQueue"));
			parameters.add(new BasicNameValuePair("org.apache.struts.taglib.html.TOKEN",token));
			parameters.add(new BasicNameValuePair("leftTicketStr",ticketNo));
			parameters.add(new BasicNameValuePair("textfield","中文或拼音首字母"));
			//一个人只有一个checkbox0
			parameters.add(new BasicNameValuePair("checkbox0","0"));
			parameters.add(new BasicNameValuePair("checkbox2","2"));
			parameters.add(new BasicNameValuePair("orderRequest.train_date",date));
			parameters.add(new BasicNameValuePair("orderRequest.train_no", params[3]));
			parameters.add(new BasicNameValuePair("orderRequest.station_train_code", params[0]));
			parameters.add(new BasicNameValuePair("orderRequest.from_station_telecode", params[4]));
			parameters.add(new BasicNameValuePair("orderRequest.to_station_telecode", params[5]));
			parameters.add(new BasicNameValuePair("orderRequest.seat_type_code",""));
			parameters.add(new BasicNameValuePair("orderRequest.ticket_type_order_num",""));
			
			parameters.add(new BasicNameValuePair("orderRequest.bed_level_order_num","000000000000000000000000000000"));
			parameters.add(new BasicNameValuePair("orderRequest.start_time",params[2]));
			parameters.add(new BasicNameValuePair("orderRequest.end_time",params[6]));
			parameters.add(new BasicNameValuePair("orderRequest.from_station_name",params[7]));
			parameters.add(new BasicNameValuePair("orderRequest.to_station_name",params[8]));
			parameters.add(new BasicNameValuePair("orderRequest.cancel_flag","1"));
			parameters.add(new BasicNameValuePair("orderRequest.id_mode","Y"));
			
			//订票人信息 第一个人
			parameters.add(new BasicNameValuePair("passengerTickets","3,0,1,刘文波,1,430981198702272830,,Y"));
			parameters.add(new BasicNameValuePair("oldPassengers","刘文波,1,430981198702272830"));
			parameters.add(new BasicNameValuePair("passenger_1_seat","3"));
			parameters.add(new BasicNameValuePair("passenger_1_ticket","1"));
			parameters.add(new BasicNameValuePair("passenger_1_name","刘文波"));
			parameters.add(new BasicNameValuePair("passenger_1_cardtype","1"));
			parameters.add(new BasicNameValuePair("passenger_1_cardno","430981198702272830"));
			parameters.add(new BasicNameValuePair("passenger_1_mobileno",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));

			parameters.add(new BasicNameValuePair("passengerTickets","3,0,1,刘丽,1,430181198406030024,18606521059,Y"));
			parameters.add(new BasicNameValuePair("oldPassengers","刘丽,1,430181198406030024"));
			parameters.add(new BasicNameValuePair("passenger_2_seat","3"));
			parameters.add(new BasicNameValuePair("passenger_2_ticket","1"));
			parameters.add(new BasicNameValuePair("passenger_2_name","刘丽"));
			parameters.add(new BasicNameValuePair("passenger_2_cardtype","1"));
			parameters.add(new BasicNameValuePair("passenger_2_cardno","430181198406030024"));
			parameters.add(new BasicNameValuePair("passenger_2_mobileno","18606521059"));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("oldPassengers",""));
			parameters.add(new BasicNameValuePair("checkbox9","Y"));
			parameters.add(new BasicNameValuePair("orderRequest.reserve_flag","A"));
			parameters.add(new BasicNameValuePair("tFlag","dc"));
			parameters.add(new BasicNameValuePair("randCode",rangCode));
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/confirmPassengerAction.do");
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URI uri = builder.build();
			HttpPost httpPost = new HttpPost(uri);
			httpPost.setEntity(uef);
			httpPost.addHeader("Accept","application/json, text/javascript, */*");
			httpPost.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
			httpPost.addHeader("Connection","keep-alive");
			httpPost.addHeader("Content-Type","application/x-www-form-urlencoded");
			httpPost.addHeader("Origin","https://dynamic.12306.cn");
			httpPost.addHeader("Accept-Language","zh-CN,zh;q=0.8");
			httpPost.addHeader("Host","dynamic.12306.cn");
			httpPost.addHeader("Referer","https://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=init");
			httpPost.addHeader("X-Requested-With","XMLHttpRequest");
			httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17");
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				JSONObject jsonObject = JSONObject.parseObject(EntityUtils.toString(entity));
				System.out.println(jsonObject.toJSONString());
				String errorMessage = jsonObject.getString("errMsg");
				if("Y".equals(errorMessage)){
					System.out.println("订票成功了，赶紧付款吧!");
				}else if(StringUtils.contains(errorMessage,"验证码")){
					checkOrderInfo(ticketNo,seatNum,token,params,date);
				}else if(StringUtils.contains(errorMessage,"排队人数现已超过余票数")){
					searchTicket(date);
				}else if(StringUtils.contains(errorMessage,"非法的订票请求")){
					searchTicket(date);
				}else{
					searchTicket(date);
				}
			}
		} catch (Exception e) {
		   e.printStackTrace();
		   
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	/**
	 * 获取登录验证码
	 * @param uri
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getRandCode(UrlEnum urlEnum){
		HttpGet httpGet = new HttpGet("https://dynamic.12306.cn/otsweb/"+urlEnum.getPath());
		InputStream inputStream = null;
		OutputStream outputStream = null;
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				inputStream = response.getEntity().getContent();
				String fileDir = this.getClass().getClassLoader().getResource(".").getPath();
				File file = new File(fileDir+"/rangcode/code.jpg");
				outputStream = new BufferedOutputStream(new FileOutputStream(file));
				IOUtils.copy(inputStream, outputStream);
			}else{
				getRandCode(urlEnum);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
			IOUtils.closeQuietly(outputStream);
		}
		System.out.print("请输入验证码:");
		Scanner scanner = new Scanner(System.in);
		String randCode = scanner.nextLine();
		if("N".equals(randCode.toUpperCase())){
			getRandCode(urlEnum);
		}else{
			return randCode;
		}
		return null;
	}

}