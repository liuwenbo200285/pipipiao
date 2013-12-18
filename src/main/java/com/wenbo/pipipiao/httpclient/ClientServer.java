package com.wenbo.pipipiao.httpclient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
import org.jsoup.Jsoup;
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
import com.wenbo.pipipiao.enumutil.UrlNewEnum;
import com.wenbo.pipipiao.util.ConfigUtil;
import com.wenbo.pipipiao.util.HttpClientUtil;

public class ClientServer {
	
	
	private static DefaultHttpClient httpClient = null;
	
	private static Logger logger = LoggerFactory.getLogger(ClientServer.class);
	
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
    
    private static Map<String,UserInfo> userInfoMap = null;
    
    private static ConfigInfo configInfo;
    
    private static UserInfo userInfo;
    
    private static JSONObject jObject;
    
    private static String key_check_isChange;
    
    private static String code;
    
    private static JSONArray seatObjectArray;
	
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
//			logger.error("httpclient error!",exception);
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
		PoolingClientConnectionManager  cm = new PoolingClientConnectionManager();
		cm.setMaxTotal(200);
		cm.setDefaultMaxPerRoute(20);
		httpClient = new DefaultHttpClient(cm);
		httpClient = (DefaultHttpClient) WebClientDevWrapper.wrapClient(httpClient);
		httpClient.getCookieSpecs().register("easy", csf);
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
		httpClient.setHttpRequestRetryHandler(myRetryHandler);
		//请求超时
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000); 
		//读取超时 
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000); 
//		httpClient.getParams().setParameter(CoreConnectionPNames, value);
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
//		new RobTicket(configInfo, userInfo, userInfoMap, httpClient).getLoginRand();
		test(httpClient);
		logger.info("程序执行完毕~~~~~~~~~~~~");
	}	
	
	public static void test(HttpClient httpClient){
		HttpResponse response;
		try {
			String username = "liuwenbo200285";
			String password = "2580233";
			String randCode = getRandCode(UrlNewEnum.LOGIN_RANGCODE_URL);
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("loginUserDTO.user_name",username));
			parameters.add(new BasicNameValuePair("userDTO.password", password));
			parameters.add(new BasicNameValuePair("randCode", randCode)); 
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.LONGIN_CONFIM);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
				JSONObject jsonObject = JSON.parseObject(info);
				logger.info(jsonObject.getString("messages"));
				if("Y".equals(jsonObject.getJSONObject("data").getString("loginCheck"))){
					searchOrder();
				}
			}
		}catch (Exception e) {
			logger.error("Login","登录出错!",e);
		}
	}
	
	/**
	 * 获取登录验证码
	 * 
	 * @param uri
	 * @return
	 * @throws URISyntaxException
	 */
	public static String getRandCode(UrlNewEnum urlEnum) {
		HttpGet httpGet = new HttpGet(UrlNewEnum.DO_MAIN.getPath()+ urlEnum.getPath());
		InputStream inputStream = null;
		OutputStream outputStream = null;
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				inputStream = response.getEntity().getContent();
				String fileDir = ClientServer.class.getClassLoader()
						.getResource(".").getPath();
				File file = new File(fileDir + "/rangcode/code.jpg");
				outputStream = new BufferedOutputStream(new FileOutputStream(
						file));
				IOUtils.copy(inputStream, outputStream);
			} else {
				HttpClientUtils.closeQuietly(response);
				IOUtils.closeQuietly(outputStream);
				getRandCode(urlEnum);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpClientUtils.closeQuietly(response);
			IOUtils.closeQuietly(outputStream);
		}
		logger.info("请输入验证码:");
		Scanner scanner = new Scanner(System.in);
		String randCode = scanner.nextLine();
		if ("N".equals(randCode.toUpperCase())) {
			getRandCode(urlEnum);
		} else {
			return randCode;
		}
		return null;
	}
	
	public static void getPassengers(HttpClient httpClient){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("pageIndex","1"));
			parameters.add(new BasicNameValuePair("pageSize","100"));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.GET_ORDER_PERSON);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
				JSONObject jsonObject = JSON.parseObject(info);
				JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("datas");
				logger.info(jsonArray.size()+"");
				for(int i = 0; i < jsonArray.size(); i++){
					logger.info(jsonArray.getJSONObject(i).toJSONString());
				}
				submitinit();
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void searchOrder(){
		HttpResponse response;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("kyfw.12306.cn/otn/")
					.setPath(UrlNewEnum.SEARCH_TICKET.getPath())
					.setParameter("leftTicketDTO.train_date", "2013-12-26")
					.setParameter("leftTicketDTO.from_station","BJQ")
					.setParameter("leftTicketDTO.to_station","AEQ")
					.setParameter("purpose_codes","ADULT");
			URI uri = builder.build();
			HttpGet httpGet = HttpClientUtil.getNewHttpGet(uri,UrlNewEnum.SEARCH_TICKET);
			response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				JSONObject jsonObject = JSONObject.parseObject(info);
				JSONArray jsonArray = jsonObject.getJSONArray("data");
				JSONObject object = null;
				for(int i = 0; i < jsonArray.size(); i++){
					object = jsonArray.getJSONObject(i);
					if("K9076".equals(object.getJSONObject("queryLeftNewDTO").getString("station_train_code"))){
						jObject = object;
						break;
					}
				}
				submitOrderRequest(object.getString("secretStr"));
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void checkuser(){
		HttpResponse response;
		try {
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.CHECKUSER);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void queryMyOrderNoComplete(HttpClient httpClient){
		HttpResponse response;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("kyfw.12306.cn/otn/")
					.setPath(UrlNewEnum.QUERY_MYORDER_NOCOMPLETE.getPath())
					.setParameter("_json_att","");
			URI uri = builder.build();
			HttpGet httpGet = HttpClientUtil.getNewHttpGet(uri,UrlNewEnum.QUERY_MYORDER_NOCOMPLETE);
			response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
				JSONObject jsonObject = JSON.parseObject(info);
				JSONArray array = jsonObject.getJSONObject("data").getJSONArray("orderDBList");
				if(array.size() > 0){
					for(int i = 0; i <array.size(); i++){
						logger.info(array.get(i).toString());
					}
				}
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void submitinit(){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("random","1387009135876"));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.SUBMITINIT);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void submitOrderRequest(String secretStr){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("secretStr",URLDecoder.decode(secretStr)));
			parameters.add(new BasicNameValuePair("train_date","2013-12-26"));
			parameters.add(new BasicNameValuePair("back_train_date","2013-12-31"));
			parameters.add(new BasicNameValuePair("tour_flag","dc"));
			parameters.add(new BasicNameValuePair("purpose_codes","ADULT"));
			parameters.add(new BasicNameValuePair("query_from_station_name","深圳"));
			parameters.add(new BasicNameValuePair("query_to_station_name","益阳"));
			parameters.add(new BasicNameValuePair("undefined",""));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.SUBMITORDERREQUEST);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				JSONObject jsonObject = JSON.parseObject(info);
				if(jsonObject.getBooleanValue("status")){
					initDc();
				}else{
					logger.info(jsonObject.getString("messages"));
				}
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void initDc(){
		HttpResponse response;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("kyfw.12306.cn/otn/")
					.setPath(UrlNewEnum.INITDC.getPath());
			URI uri = builder.build();
			HttpGet httpGet = HttpClientUtil.getNewHttpGet(uri,UrlNewEnum.INITDC);
			response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				String str = EntityUtils.toString(response.getEntity());
				//解析token
				int n = StringUtils.indexOf(str,"globalRepeatSubmitToken");
				String info = StringUtils.substring(str,n+27,n+59);
				n = StringUtils.indexOf(str,"init_seatTypes");
				int m = StringUtils.indexOf(str,";",n);
				String ticket = StringUtils.substring(str,n+15,n+(m-n));
				seatObjectArray = JSON.parseArray(ticket);
				n = StringUtils.indexOf(str,"key_check_isChange");
				key_check_isChange = StringUtils.substring(str,n+21,n+77);
				code = getRandCode(UrlNewEnum.PASSENGER_RANGCODE);
				checkOrderInfo(info);
				logger.info(info);
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
			e.printStackTrace();
		}
	}
	
	public static void checkOrderInfo(String token){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("cancel_flag","2"));
			parameters.add(new BasicNameValuePair("bed_level_order_num","000000000000000000000000000000"));
			parameters.add(new BasicNameValuePair("passengerTicketStr","1,0,1,刘文波,1,430981198702272830,18606521059,N"));
			parameters.add(new BasicNameValuePair("oldPassengerStr","刘文波,1,430981198702272830,1_"));
			parameters.add(new BasicNameValuePair("tour_flag","dc"));
			parameters.add(new BasicNameValuePair("randCode",code));
			parameters.add(new BasicNameValuePair("_json_att",""));
			parameters.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN",token));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.CHECKORDERINFO);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				JSONObject jsonObject = JSON.parseObject(info);
				if(jsonObject.getBooleanValue("status")
						&& jsonObject.getJSONObject("data").getBooleanValue("submitStatus")){
					getQueueCount(token);
				}else{
					logger.info(info);
				}
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void getQueueCount(String token){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("train_date",new SimpleDateFormat("yyyy-MM-dd",Locale.CHINESE).parse("2013-12-26").toString()));
			parameters.add(new BasicNameValuePair("train_no",jObject.getJSONObject("queryLeftNewDTO").getString("train_no")));
			parameters.add(new BasicNameValuePair("stationTrainCode",jObject.getJSONObject("queryLeftNewDTO").getString("station_train_code")));
			parameters.add(new BasicNameValuePair("seatType","1"));
			parameters.add(new BasicNameValuePair("fromStationTelecode",jObject.getJSONObject("queryLeftNewDTO").getString("from_station_telecode")));
			parameters.add(new BasicNameValuePair("toStationTelecode",jObject.getJSONObject("queryLeftNewDTO").getString("to_station_telecode")));
			parameters.add(new BasicNameValuePair("leftTicket",jObject.getJSONObject("queryLeftNewDTO").getString("yp_info")));
			parameters.add(new BasicNameValuePair("purpose_codes","00"));
			parameters.add(new BasicNameValuePair("_json_att",""));
			parameters.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN",token));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.GETQUEUECOUNT);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
				JSONObject jsonObject = JSON.parseObject(info);
				if(jsonObject.getBooleanValue("status")){
					confirmSingleForQueue(token,jsonObject.getJSONObject("data").getString("ticket"));
				}else{
					logger.info(jsonObject.getString("messages"));
				}
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
	public static void confirmSingleForQueue(String token,String ticket){
		HttpResponse response;
		try {
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("passengerTicketStr","1,0,1,刘文波,1,430981198702272830,18606521059,N"));
			parameters.add(new BasicNameValuePair("oldPassengerStr","刘文波,1,430981198702272830,1_"));
			parameters.add(new BasicNameValuePair("randCode",code));
			parameters.add(new BasicNameValuePair("purpose_codes","00"));
			parameters.add(new BasicNameValuePair("key_check_isChange",key_check_isChange));
			parameters.add(new BasicNameValuePair("leftTicketStr",ticket));
			parameters.add(new BasicNameValuePair("train_location",jObject.getJSONObject("queryLeftNewDTO").getString("location_code")));
			parameters.add(new BasicNameValuePair("_json_att",""));
			parameters.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN",token));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters,"UTF-8");
			HttpPost httpPost = HttpClientUtil.getNewHttpPost(UrlNewEnum.CONFIRMSINGLEFORQUEUE);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if (response.getStatusLine().getStatusCode() == 302) {
			} else if (response.getStatusLine().getStatusCode() == 404) {
			} else if (response.getStatusLine().getStatusCode() == 200) {
				String info = EntityUtils.toString(response.getEntity());
				logger.info(info);
				JSONObject jsonObject = JSON.parseObject(info);
				if(jsonObject.getBooleanValue("status")){
					
				}else{
					logger.info(jsonObject.getString("messages"));
				}
			}
		}catch (Exception e) {
			logger.error("Login","获取用户联系人出错!",e);
		}
	}
	
}

