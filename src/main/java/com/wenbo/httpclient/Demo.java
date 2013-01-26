package com.wenbo.httpclient;

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
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
<<<<<<< HEAD
=======
import org.apache.http.client.HttpRequestRetryHandler;
>>>>>>> 0127
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
<<<<<<< HEAD
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
=======
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
>>>>>>> 0127
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class Demo {
	
	
	private static DefaultHttpClient httpClient = null;
	
	private static Logger logger = LoggerFactory.getLogger(Demo.class);
	
<<<<<<< HEAD
	private static final String RANG_CODE_PATH = "F:/work/12306/rangCode.jpg";
	
	private static final String REFER = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
=======
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
>>>>>>> 0127
	
	//确定预定验证码 https://dynamic.12306.cn/otsweb/passCodeAction.do?rand=randp
	/**
	 * 避免HttpClient的”SSLPeerUnverifiedException: peer not authenticated”异常
	 * 不用导入SSL证书
	 *
	 *
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
		HttpParams httpParams = httpClient.getParams(); 
        // 设置连接超时时间  
        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);  
        // 设置读取超时时间  
        HttpConnectionParams.setSoTimeout(httpParams, READ_TIMEOUT);  
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
		 // Prepare a request object
		getLoginRand();
//		test();
	}
	
	
<<<<<<< HEAD
	public static void test(){
=======
	public static boolean test() throws IllegalStateException, IOException{
>>>>>>> 0127
	    try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/confirmPassengerAction.do")
		    .setParameter("method","getQueueCount")
<<<<<<< HEAD
		    .setParameter("train_date","2013-02-13")
		    .setParameter("train_no","6c000G601108")
		    .setParameter("station","G6011")
=======
		    .setParameter("train_date","2013-02-14")
		    .setParameter("train_no","62000K901709")
		    .setParameter("station","K9017")
>>>>>>> 0127
		    .setParameter("seat","O")
		    .setParameter("from","CWQ")
		    .setParameter("to","IOQ")
		    .setParameter("ticket","O038850183M060350021P071950000");
			URI uri = builder.build();
			HttpGet httpGet = new HttpGet(uri);
			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
			httpGet.addHeader("Connection","keep-alive");
			httpGet.addHeader("Cache-Control","max-age=0");
			httpGet.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8");
			httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
			httpGet.addHeader("Host","dynamic.12306.cn");
<<<<<<< HEAD
			printlnResponseData(httpClient.execute(httpGet));
		} catch (Exception e) {
			e.printStackTrace();
		}
=======
			response = httpClient.execute(httpGet);
			Document document = JsoupUtil.getPageDocument(response.getEntity().getContent());
			if(document.getElementById("loginForm") == null){
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
		return false;
>>>>>>> 0127
	}
	
	/**
	 * 获取登录码
	 */
	public static void getLoginRand(){
		try {
<<<<<<< HEAD
			HttpGet httpget = new HttpGet("https://dynamic.12306.cn/otsweb/loginAction.do?method=loginAysnSuggest");
			 HttpResponse response = httpClient.execute(httpget);
			 System.out.println(response.getStatusLine());
			 HttpEntity entity = response.getEntity();
			 if (entity != null) {
			     InputStream instream = entity.getContent();
			     try {
			         BufferedReader reader = new BufferedReader(
=======
			HttpGet httpget = new HttpGet(URL_HEAD+UrlEnum.LOGIN_INIT_URL.getPath());
			response = httpClient.execute(httpget);
			HttpEntity entity = response.getEntity();
			 if (entity != null && entity.getContentLength() > 0) {
			     InputStream instream = entity.getContent();
			     BufferedReader reader = null;
			     try {
			    	 reader = new BufferedReader(
>>>>>>> 0127
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
<<<<<<< HEAD
			         instream.close();
			     }
			     httpClient.getConnectionManager().shutdown();
=======
			    	 HttpClientUtils.closeQuietly(response);
			         IOUtils.closeQuietly(reader);
//			         httpClient.getConnectionManager().releaseConnection(conn, validDuration, timeUnit)
			     }
//			     httpClient.getConnectionManager().shutdown();
>>>>>>> 0127
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void login(String loginRand,String randError) throws ClientProtocolException, IOException{
		System.out.println("loginRand:"+loginRand+",randError:"+randError);
		//获取验证码
		try {
<<<<<<< HEAD
			String randCode = getRandCode(new URI("https://dynamic.12306.cn/otsweb/passCodeAction.do?rand=sjrand"));
			
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/loginAction.do")
			    .setParameter("method", "login")
			    .setParameter("loginRand",loginRand)
			    .setParameter("refundLogin", "N")
			    .setParameter("refundFlag", "Y")
			    .setParameter("loginUser.user_name", "liuwenbo201085")
			    .setParameter("nameErrorFocus", "")
			    .setParameter("user.password", "liuwenbo520")
			    .setParameter("randCode",randCode)
			    .setParameter("randErrorFocus","");
=======
			String randCode = getRandCode(UrlEnum.LOGIN_RANGCODE_URL);
//			URIBuilder builder = new URIBuilder();
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters.add(new BasicNameValuePair("method","login"));
			parameters.add(new BasicNameValuePair("loginRand",loginRand));
			parameters.add(new BasicNameValuePair("refundLogin","N"));
			parameters.add(new BasicNameValuePair("refundFlag", "Y"));
			parameters.add(new BasicNameValuePair("loginUser.user_name","liuwenbo201085"));
			parameters.add(new BasicNameValuePair("nameErrorFocus",""));
			parameters.add(new BasicNameValuePair("user.password","liuwenbo520"));
			parameters.add(new BasicNameValuePair("randCode",randCode));
			parameters.add(new BasicNameValuePair("randErrorFocus",""));
			UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.LONGIN_CONFIM.getPath());
>>>>>>> 0127
			URI uri = builder.build();
			HttpPost httpPost = getHttpPost(uri,UrlEnum.LONGIN_CONFIM);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
<<<<<<< HEAD
				printlnResponseData(response);
				Header locationHeader = response.getFirstHeader("Location");
		        if (locationHeader != null) {
		            String redirectUrl = locationHeader.getValue();
		            uri = new URI(redirectUrl);
		            HttpGet httpGet = getHttpGet(uri,"");
		            response = httpClient.execute(httpGet);
		            printlnResponseData(response);
		        }
			}else{
				if(JsoupUtil.validateLogin(response.getEntity().getContent())){
					System.out.println("登录成功,开始刷票!");
//					searchTicket("2013-02-13");
					test();
				}else{
					getLoginRand();
				}
=======
//				printlnResponseData(response);
			}else if(response.getStatusLine().getStatusCode() == 404){
//				printlnResponseData(response);
			}else if(response.getStatusLine().getStatusCode() == 200){
//				printlnResponseData(response);
				searchTicket("2013-02-14");
>>>>>>> 0127
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
	public static void searchTicket(String date){
		HttpGet httpGet = null;
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/querySingleAction.do")
			    .setParameter("method","queryLeftTicket")
			    .setParameter("orderRequest.train_date",date)
<<<<<<< HEAD
			    .setParameter("orderRequest.from_station_telecode", "CWQ")
			    .setParameter("orderRequest.to_station_telecode", "SZQ")
			    .setParameter("orderRequest.train_no", "6c000G601108")
=======
			    .setParameter("orderRequest.from_station_telecode", "CSQ")
			    .setParameter("orderRequest.to_station_telecode", "IOQ")
			    .setParameter("orderRequest.train_no", "62000K901709")
>>>>>>> 0127
			    .setParameter("trainPassType","QB")
			    .setParameter("trainClass", "K#")
			    .setParameter("includeStudent","00")
			    .setParameter("seatTypeAndNum","")
			    .setParameter("orderRequest.start_time_str","00:00--24:00");
			URI uri = builder.build();
<<<<<<< HEAD
			String searchRefer = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
			httpGet = getHttpGet(uri,searchRefer);
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				checkTickeAndOrder(response,date);
=======
			httpGet = getHttpGet(uri,UrlEnum.SEARCH_TICKET);
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				checkTickeAndOrder(response,date);
//				printlnResponseData(response);
>>>>>>> 0127
			}
		} catch (Exception e) {
			httpGet.abort();
			e.printStackTrace();
<<<<<<< HEAD
=======
		}finally{
			
>>>>>>> 0127
		}
	}
	
	
	/**
	 * 检测出票结果以及刷票
	 * @param inputStream
<<<<<<< HEAD
	 */
	public static void checkTickeAndOrder(HttpResponse response,String date){
		Document document = null;
		try {
			document = JsoupUtil.turnHtmlToDocument(response.getEntity().getContent());
			if(true){
=======
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public static void checkTickeAndOrder(HttpResponse response,String date) throws IllegalStateException, IOException{
		Document document = null;
		try {
			document = JsoupUtil.getPageDocument(response.getEntity().getContent());
			if(document.getElementsContainingText("-10").size() != 0){
				System.out.println(document.toString());
				getLoginRand();
			}
			if(JsoupUtil.checkHaveTicket(document,10)){
>>>>>>> 0127
				System.out.println("有票了，开始订票~~~~~~~~~");
				String[] params = JsoupUtil.getTicketInfo(document);
				orderTicket(date,params);
			}
<<<<<<< HEAD
//			else{
//				System.out.println("休息一秒，继续刷票");
//				Thread.sleep(1000);
//				searchTicket(date);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			
		}
	}
	
	public static void orderTicket(String date,String[] params){
=======
			else{
				System.out.println("休息一秒，继续刷票");
				Thread.sleep(1000);
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
>>>>>>> 0127
		HttpPost httpPost = null;
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
<<<<<<< HEAD
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/querySingleAction.do");
			URI uri = builder.build();
			String searchRefer = "http://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=init";
			httpPost = getHttpPost(uri, searchRefer);
			httpPost.setEntity(uef);
			HttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
				Header locationHeader = response.getFirstHeader("Location");
				getLeftTicket(locationHeader.getValue(), params,date);
=======
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.BOOK_TICKET.getPath());
			URI uri = builder.build();
			httpPost = getHttpPost(uri,UrlEnum.BOOK_TICKET);
			httpPost.setEntity(uef);
			response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
//				Header locationHeader = response.getFirstHeader("Location");
////				locationHeader.
//				getLeftTicket(locationHeader.getValue(), params,date);
//				httpPost.setURI(new URI(locationHeader.getValue()));
//				response = httpClient.execute(httpPost);
//				printlnResponseData(response);
>>>>>>> 0127
			}else if(response.getStatusLine().getStatusCode() == 200){
				System.out.println("非法订票请求!!!!!");
				printlnResponseData(response);
			}
		} catch (Exception e) {
			httpPost.abort();
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	
	/**
	 * 获取ticketNo
	 * @param url
	 * @param params
	 */
	public static void getLeftTicket(String url,String[] params,String date){
		HttpGet httpGet = null;
		try {
<<<<<<< HEAD
			URI uri = new URI(url);
			httpGet = getHttpGet(uri, REFER);
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				Document document = Jsoup.parse(response.getEntity().getContent(),"UTF-8","https://dynamic.12306.cn/");
				Element element = document.getElementById("left_ticket");
				if(element != null){
					String ticketNo = element.attr("value");
					checkTicket(ticketNo,params,date);
				}
				
			}
		} catch (Exception e) {
			httpGet.abort();
			e.printStackTrace();
=======
			System.out.println();
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath(UrlEnum.GET_SEAT_VALUE.getPath());
			URI uri = builder.build();
			httpGet = getHttpGet(uri,UrlEnum.GET_SEAT_VALUE);
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				Document document = JsoupUtil.getPageDocument(response.getEntity().getContent());
				Element element = document.getElementById("left_ticket");
				if(element != null){
					String ticketNo = element.attr("value");
//					checkTicket(ticketNo,params,date);
				}
				element = document.getElementById("passenger_1_seat");
				
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			HttpClientUtils.closeQuietly(response);
>>>>>>> 0127
		}
	}
	
	/**
	 * 检测有没有票
	 * @param ticketNo
	 * @param params
	 */
<<<<<<< HEAD
	public static void checkTicket(String ticketNo,String[] params,String date){
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/confirmPassengerAction.do")
			    .setParameter("method","getQueueCount")
			    .setParameter("train_date",date)
			    .setParameter("train_no",params[3])
			    .setParameter("station",params[0])
			    .setParameter("seat","O")
			    .setParameter("from", params[4])
			    .setParameter("to", params[5])
			    .setParameter("ticket",ticketNo);
			URI uri = builder.build();
			HttpGet httpGet = getHttpGet(uri,"https://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=init");
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				printlnResponseData(response);
//				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"UTF-8"));
//				String ticketInfo = bufferedReader.readLine();
//				System.out.println(ticketInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			
		}
	}
=======
//	public static void checkTicket(String ticketNo,String[] params,String date){
//		try {
//			URIBuilder builder = new URIBuilder();
//			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/confirmPassengerAction.do")
//			    .setParameter("method","getQueueCount")
//			    .setParameter("train_date",date)
//			    .setParameter("train_no",params[3])
//			    .setParameter("station",params[0])
//			    .setParameter("seat","O")
//			    .setParameter("from", params[4])
//			    .setParameter("to", params[5])
//			    .setParameter("ticket",ticketNo);
//			URI uri = builder.build();
//			HttpGet httpGet = getHttpGet(uri,"https://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=init");
//			HttpResponse response = httpClient.execute(httpGet);
//			if(response.getStatusLine().getStatusCode() == 200){
//				printlnResponseData(response);
////				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"UTF-8"));
////				String ticketInfo = bufferedReader.readLine();
////				System.out.println(ticketInfo);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}finally{
//			
//		}
//	}
>>>>>>> 0127
	
	public static void printlnResponseData(HttpResponse response){
		System.out.println(response.getStatusLine());
		System.out.println(response.getEntity().getContentLength());
		if(response.getEntity() != null){
			InputStream inputStream;
			try {
				inputStream = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
		                 new InputStreamReader(inputStream,"UTF-8"));
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
		httpPost.addHeader("Accept-Encoding","gzip,deflate,sdch");
		httpPost.addHeader("Cache-Control","max-age=0");
		httpPost.addHeader("Connection","keep-alive");
		httpPost.addHeader("Origin","https://dynamic.12306.cn");
		httpPost.addHeader("Host","dynamic.12306.cn");
		httpPost.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
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
	
	public static HttpGet getHttpGet(URI uri,UrlEnum urlEnum){
		HttpGet httpGet = new HttpGet(uri);
		if(StringUtils.isNotEmpty(urlEnum.getAccept())){
			httpGet.addHeader("Accept",urlEnum.getAccept());
		}
		httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpGet.addHeader("Accept-Encoding","gzip,deflate,sdch");
		httpGet.addHeader("Cache-Control","max-age=0");
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
//		httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		return httpGet;
	}
	
	
	/**
	 * 获取登录验证码
	 * @param uri
	 * @return
	 * @throws URISyntaxException 
	 */
	public static String getRandCode(UrlEnum urlEnum) throws URISyntaxException{
//		URIBuilder builder = new URIBuilder();
//		builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/passCodeAction.do")
//		.setParameter("rand","sjrand");
//		URI uri = builder.build();
		HttpGet httpGet = new HttpGet("https://dynamic.12306.cn/otsweb/passCodeAction.do?rand=sjrand");
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				inputStream = response.getEntity().getContent();
				File file = new File(RANG_CODE_PATH);
<<<<<<< HEAD
				byte[] b = new byte[1024];
				outputStream = new BufferedOutputStream(new FileOutputStream(file));
				while(inputStream.read(b) != -1){
					outputStream.write(b);
				}
				outputStream.flush();
				System.out.print("请输入验证码:");
				Scanner scanner = new Scanner(System.in);
				String randCode = scanner.nextLine();
				if("N".equals(randCode.toUpperCase())){
					getRandCode(uri);
				}else{
					return randCode;
				}
			}else{
				Thread.sleep(2000);
				getRandCode(uri);
=======
				outputStream = new BufferedOutputStream(new FileOutputStream(file));
				IOUtils.copy(inputStream, outputStream);
			}else{
				Thread.sleep(2000);
				getRandCode(urlEnum);
>>>>>>> 0127
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

