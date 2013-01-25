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
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class Demo {
	
	
	private static DefaultHttpClient httpClient = null;
	
	private static Logger logger = LoggerFactory.getLogger(Demo.class);
	
	private static final String RANG_CODE_PATH = "F:/work/12306/rangCode.jpg";
	
	private static final String REFER = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
	
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
		httpClient = new DefaultHttpClient();
		httpClient = (DefaultHttpClient) WebClientDevWrapper.wrapClient(httpClient);
		httpClient.getCookieSpecs().register("easy", csf);
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
		 // Prepare a request object
		getLoginRand();
//		test();
	}
	
	
	public static void test(){
	    try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/confirmPassengerAction.do")
		    .setParameter("method","getQueueCount")
		    .setParameter("train_date","2013-02-13")
		    .setParameter("train_no","6c000G601108")
		    .setParameter("station","G6011")
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
			printlnResponseData(httpClient.execute(httpGet));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取登录码
	 */
	public static void getLoginRand(){
		try {
			HttpGet httpget = new HttpGet("https://dynamic.12306.cn/otsweb/loginAction.do?method=loginAysnSuggest");
			 HttpResponse response = httpClient.execute(httpget);
			 System.out.println(response.getStatusLine());
			 HttpEntity entity = response.getEntity();
			 if (entity != null) {
			     InputStream instream = entity.getContent();
			     try {
			         BufferedReader reader = new BufferedReader(
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
			         instream.close();
			     }
			     httpClient.getConnectionManager().shutdown();
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void login(String loginRand,String randError) throws ClientProtocolException, IOException{
		System.out.println("loginRand:"+loginRand+",randError:"+randError);
		//获取验证码
		try {
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
			URI uri = builder.build();
			String loginReferer = "https://dynamic.12306.cn/otsweb/loginAction.do?method=login";
			HttpPost httpPost = getHttpPost(uri,loginReferer);
			HttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
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
			    .setParameter("orderRequest.train_no", "6c000G601108")
			    .setParameter("trainPassType","QB")
			    .setParameter("trainClass", "D#")
			    .setParameter("includeStudent","00")
			    .setParameter("seatTypeAndNum","")
			    .setParameter("orderRequest.start_time_str","00:00--24:00");
			URI uri = builder.build();
			String searchRefer = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
			httpGet = getHttpGet(uri,searchRefer);
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				checkTickeAndOrder(response,date);
			}
		} catch (Exception e) {
			httpGet.abort();
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 检测出票结果以及刷票
	 * @param inputStream
	 */
	public static void checkTickeAndOrder(HttpResponse response,String date){
		Document document = null;
		try {
			document = JsoupUtil.turnHtmlToDocument(response.getEntity().getContent());
			if(true){
				System.out.println("有票了，开始订票~~~~~~~~~");
				String[] params = JsoupUtil.getTicketInfo(document);
				orderTicket(date,params);
			}
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
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/querySingleAction.do");
			URI uri = builder.build();
			String searchRefer = "http://dynamic.12306.cn/otsweb/order/confirmPassengerAction.do?method=init";
			httpPost = getHttpPost(uri, searchRefer);
			httpPost.setEntity(uef);
			HttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == 302){
				Header locationHeader = response.getFirstHeader("Location");
				getLeftTicket(locationHeader.getValue(), params,date);
			}else if(response.getStatusLine().getStatusCode() == 200){
				System.out.println("非法订票请求!!!!!");
				printlnResponseData(response);
			}
		} catch (Exception e) {
			httpPost.abort();
			e.printStackTrace();
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
		}
	}
	
	/**
	 * 检测有没有票
	 * @param ticketNo
	 * @param params
	 */
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
	
	public static void printlnResponseData(HttpResponse response){
		System.out.println(response.getStatusLine());
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
	public static HttpPost getHttpPost(URI uri,String referer){
		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
		httpPost.addHeader("Connection","keep-alive");
		httpPost.addHeader("Cache-Control","max-age=0");
		httpPost.addHeader("Accept","application/json, text/javascript, */*");
		httpPost.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		httpPost.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpPost.addHeader("X-Requested-With","XMLHttpRequest");
		httpPost.addHeader("Content-Type","application/x-www-form-urlencoded");
		httpPost.addHeader("Host","dynamic.12306.cn");
		httpPost.addHeader("Origin","https://dynamic.12306.cn");
		httpPost.addHeader("Referer",referer);
		return httpPost;
	}
	
	public static HttpGet getHttpGet(URI uri,String referer){
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17");
		httpGet.addHeader("Connection","keep-alive");
		httpGet.addHeader("Cache-Control","max-age=0");
		httpGet.addHeader("Accept","application/json, text/javascript, */*");
		httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		httpGet.addHeader("Accept-Charset","GBK,utf-8;q=0.7,*;q=0.3");
		httpGet.addHeader("X-Requested-With","XMLHttpRequest");
		httpGet.addHeader("Content-Type","application/x-www-form-urlencoded");
		httpGet.addHeader("Host","dynamic.12306.cn");
		httpGet.addHeader("Origin","https://dynamic.12306.cn");
		httpGet.addHeader("Referer",referer);
		return httpGet;
	}
	
	
	/**
	 * 获取登录验证码
	 * @param uri
	 * @return
	 */
	public static String getRandCode(URI uri){
		HttpGet httpGet = getHttpGet(uri,"");
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				inputStream = response.getEntity().getContent();
				File file = new File(RANG_CODE_PATH);
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
			}
		} catch (Exception e) {
			httpGet.abort();
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
		}
		return null;
	}
	
	
	/**
	 * 获取页面对象
	 * @param url
	 * @return
	 */
	public static  Document getPageDocument(InputStream inputStream) {
		Document doc = null;
		try {
			BufferedReader reader = new BufferedReader(
	                 new InputStreamReader(inputStream));
			String str = null;
			StringBuilder sbBuilder = new StringBuilder();
			while((str = reader.readLine()) != null){
				sbBuilder.append(str);
			}
			doc = Jsoup.parse(sbBuilder.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(inputStream);
		}
		return doc;
	}
	
	
	
	
	
	
	
	
	
	
}

