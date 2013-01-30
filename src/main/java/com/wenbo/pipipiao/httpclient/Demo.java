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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wenbo.pipipiao.domain.ConfigInfo;
import com.wenbo.pipipiao.domain.UserInfo;
import com.wenbo.pipipiao.enumutil.UrlEnum;
import com.wenbo.pipipiao.util.ConfigUtil;

public class Demo {
	
	
	private static DefaultHttpClient httpClient = null;
	
	private static Logger logger = LoggerFactory.getLogger(Demo.class);
	
	private static final String RANG_CODE_PATH = "/Users/wenbo/work/yanzhengma/rangCode.jpg";
	
	private static final String REFER = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
	
	private static final String URL_HEAD = "https://dynamic.12306.cn";
	
	 /** 
     * 最大连接数 
     */  
    public final static int MAX_TOTAL_CONNECTIONS = 800;  
    /** 
     * 获取连接的最大等待时间 
     */  
    public final static int WAIT_TIMEOUT = 60000;  
    /** 
     * 每个路由最大连接数 
     */  
    public final static int MAX_ROUTE_CONNECTIONS = 400;  
    /** 
     * 连接超时时间 
     */  
    public final static int CONNECT_TIMEOUT = 10000;  
    /** 
     * 读取超时时间 
     */  
    public final static int READ_TIMEOUT = 10000; 
    
    private static HttpResponse response = null;
    
    private static String rangCode = null;
    
    private static Map<String,UserInfo> userInfoMap = null;
    
    private static ConfigInfo configInfo;
    
    private static UserInfo userInfo;
	
	/**
	 * 避免HttpClient的”SSLPeerUnverifiedException: peer not authenticated”异常
	 * 不用导入SSL证书
	 */
	public static class WebClientDevWrapper {

	    public static org.apache.http.client.HttpClient wrapClient(org.apache.http.client.HttpClient base) {
	        try {
	            SSLContext ctx = SSLContext.getInstance("TLS");
	            X509TrustManager tm = new X509TrustManager() {
	                public X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
	                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
	            };
	            ctx.init(null, new TrustManager[] {tm}, null);
	            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	            SchemeRegistry registry = new SchemeRegistry();
	            registry.register(new Scheme("https", 443, ssf));
	            PoolingClientConnectionManager mgr = new PoolingClientConnectionManager(registry);
	            return new DefaultHttpClient(mgr, base.getParams());
	        } catch (Exception ex) {
	            ex.printStackTrace();
	            return null;
	        }
	    }
	}
	
	public static void main(String[] args) throws ClientProtocolException,
			IOException {
		//加载配置文件
		configInfo = ConfigUtil.loadConfigInfo();
		//处理cookie
		CookieSpecFactory csf = new CookieSpecFactory() {
		    public BrowserCompatSpec newInstance(HttpParams params) {
		        return new BrowserCompatSpec() {   
		        	@Override
		            public void validate(Cookie cookie, CookieOrigin origin)
		            throws MalformedCookieException {
		                // Oh, I am easy
		            }
		        };
		    }
		};
		//异常自动恢复
		HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
			public boolean retryRequest(IOException exception, int executionCount,HttpContext context) {
			if (executionCount >= 5) {
			    // 如果超过最大重试次数,那么就不要继续了
			    return false; 
			}
			if (exception instanceof NoHttpResponseException) { // 如果服务器丢掉了连接,那么就重试
				return true;
			}
			if (exception instanceof SSLHandshakeException) {
			// 不要重试SSL握手异常
				return false; 
			}
			HttpRequest request = (HttpRequest) context.getAttribute( ExecutionContext.HTTP_REQUEST);
			boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
			if (idempotent) {
			    // 如果请求被认为是幂等的,那么就重试
			    return true; 
			}
			return false;
			} };
		httpClient = new DefaultHttpClient();
		httpClient = (DefaultHttpClient) WebClientDevWrapper.wrapClient(httpClient);
		httpClient.getCookieSpecs().register("easy", csf);
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
		httpClient.setHttpRequestRetryHandler(myRetryHandler);
		//请求超时
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000); 
		//读取超时 
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000); 
		// 以下为新增内容
		httpClient.setRedirectStrategy(new DefaultRedirectStrategy() {                
		        public boolean isRedirected(HttpRequest request, HttpResponse response,HttpContext context)  {
		            boolean isRedirect=false;
		            try {
		                isRedirect = super.isRedirected(request, response, context);
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		            if (!isRedirect) {
		                int responseCode = response.getStatusLine().getStatusCode();
		                if (responseCode == 301 || responseCode == 302) {
		                    return true;
		                }
		            }
		            return isRedirect;
		        }
		});
		logger.info("开始执行程序~~~~~~~~~~~~");
		new RobTicket(configInfo, userInfo, userInfoMap, httpClient).getLoginRand();
		logger.info("程序执行完毕~~~~~~~~~~~~");
//		getLoginRand();
//		test();
	}
	
	/**
	 * 获取登录码
	 */
	public static void getLoginRand(){
		try {
			HttpGet httpget = new HttpGet(URL_HEAD+UrlEnum.LOGIN_INIT_URL.getPath());
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
			         logger.info(object.toJSONString());
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
	
	public static void login(String loginRand,String randError) throws ClientProtocolException, IOException{
		//获取验证码
		try {
			String randCode = getRandCode(UrlEnum.LOGIN_RANGCODE_URL);
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","login"));
			parameters.add(new BasicNameValuePair("loginRand",loginRand));
			parameters.add(new BasicNameValuePair("refundLogin","N"));
			parameters.add(new BasicNameValuePair("refundFlag", "Y"));
			parameters.add(new BasicNameValuePair("loginUser.user_name","liuwenbo200285"));
			parameters.add(new BasicNameValuePair("nameErrorFocus",""));
			parameters.add(new BasicNameValuePair("user.password","2580233"));
			parameters.add(new BasicNameValuePair("randCode",randCode));
			parameters.add(new BasicNameValuePair("randErrorFocus",""));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.LONGIN_CONFIM.getPath());
			URI uri = builder.build();
			HttpPost httpPost = getHttpPost(uri,UrlEnum.LONGIN_CONFIM);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
			}else if(response.getStatusLine().getStatusCode() == 404){
			}else if(response.getStatusLine().getStatusCode() == 200){
				Document document = JsoupUtil.getPageDocument(response.getEntity().getContent());
				//判断登录状态
				if(JsoupUtil.validateLogin(document)){
//					searchTicket("2013-02-10");
					getOrderPerson();
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
	public static void getOrderPerson() throws URISyntaxException{
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.GET_ORDER_PERSON.getPath())
			    .setParameter("method","getpassengerJson");
			URI uri = builder.build();
			HttpPost httpPost = getHttpPost(uri,UrlEnum.GET_ORDER_PERSON);
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
		}
	}
	
	
	/***
	 * 构造查询火车票对象
	 * @throws URISyntaxException 
	 **/
	public static void searchTicket(String date){
		HttpGet httpGet = null;
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
			httpGet = getHttpGet(uri,UrlEnum.SEARCH_TICKET);
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				checkTickeAndOrder(response,date);
			}
		} catch (Exception e) {
			httpGet.abort();
			e.printStackTrace();
		}finally{
			
		}
	}
	
	
	/**
	 * 检测出票结果以及刷票
	 * @param inputStream
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public static void checkTickeAndOrder(HttpResponse response,String date) throws IllegalStateException, IOException{
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
	public static void orderTicket(String date,String[] params) throws IllegalStateException, IOException{
		HttpPost httpPost = null;
		OutputStream outputStream = null;
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
			httpPost = getHttpPost(uri,UrlEnum.BOOK_TICKET);
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
				printlnResponseData(response);
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
	public static void checkOrderInfo(String ticketNo,String seatNum,String token,String[] params,String date){
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
			rangCode = getRandCode(UrlEnum.ORDER_RANGCODE_URL);
			parameters.add(new BasicNameValuePair("rand",rangCode));
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.GET_ORDER_INFO.getPath());
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URI uri = builder.build();
			HttpPost httpPost = getHttpPost(uri,UrlEnum.GET_ORDER_INFO);
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
						checkTicket(ticketNo,seatNum,token,params,date);
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
	public static void checkTicket(String ticketNo,String seatNum,String token,String[] params,String date){
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
				System.out.println(EntityUtils.toString(response.getEntity()));
				orderTicketToQueue(ticketNo,seatNum,token,params,date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	public static void orderTicketToQueue(String ticketNo,String seatNum,String token,String[] params,String date){
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
	
	public static void printlnResponseData(HttpResponse response){
		System.out.println(response.getStatusLine());
		System.out.println(response.getEntity().getContentLength());
		if(response.getEntity() != null){
			InputStream inputStream;
			try {
				inputStream = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
		                 new InputStreamReader(inputStream));
				String str = null;
				while((str = reader.readLine()) != null){
					System.out.println(str);
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
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
		httpPost.addHeader("Referer",REFER);
		return httpPost;
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
		httpGet.addHeader("Referer",REFER);
		return httpGet;
	}
	
	
	/**
	 * 获取登录验证码
	 * @param uri
	 * @return
	 * @throws URISyntaxException 
	 */
	public static String getRandCode(UrlEnum urlEnum){
		HttpGet httpGet = new HttpGet("https://dynamic.12306.cn/otsweb/"+urlEnum.getPath());
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				inputStream = response.getEntity().getContent();
				File file = new File(RANG_CODE_PATH);
				outputStream = new BufferedOutputStream(new FileOutputStream(file));
				IOUtils.copy(inputStream, outputStream);
			}else{
				Thread.sleep(2000);
				getRandCode(urlEnum);
			}
		} catch (Exception e) {
			httpGet.abort();
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

