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
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class Demo {
	
	
	private static DefaultHttpClient httpClient = null;
	
	private static Logger logger = LoggerFactory.getLogger(Demo.class);
	
	private static final String AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.52 Safari/537.17";
	
	//确定预定验证码 https://dynamic.12306.cn/otsweb/passCodeAction.do?rand=randp
	/**
	 * 避免HttpClient的”SSLPeerUnverifiedException: peer not authenticated”异常
	 * 不用导入SSL证书
	 * @author shipengzhi(shipengzhi@sogou-inc.com)
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
	            ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);
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
	}
	
	public static void login(String loginRand,String randError) throws ClientProtocolException, IOException{
		System.out.println("loginRand:"+loginRand+",randError:"+randError);
		//获取验证码
		try {
			getRandCode(new URI("https://dynamic.12306.cn/otsweb/passCodeAction.do?rand=sjrand"));
			System.out.print("请输入验证码：");
			Scanner scanner = new Scanner(System.in);
			String randCode = scanner.nextLine();
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/loginAction.do")
			    .setParameter("method", "login")
			    .setParameter("loginRand",loginRand)
			    .setParameter("refundLogin", "N")
			    .setParameter("refundFlag", "Y")
			    .setParameter("loginUser.user_name", "liuwenbo200285")
			    .setParameter("nameErrorFocus", "")
			    .setParameter("user.password", "2580233")
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
				printlnResponseData(response);
				searchTicket();
//				Document document = getPageDocument(response.getEntity().getContent());
//				Element element = document.getElementById("bookTicket");
//				JsoupUtil.validateLogin(response.getEntity().getContent());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/***
	 * 构造查询火车票对象
	 * @throws URISyntaxException 
	 **/
	public static void searchTicket(){
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost("dynamic.12306.cn").setPath("/otsweb/order/querySingleAction.do")
			    .setParameter("method","queryLeftTicket")
			    .setParameter("orderRequest.train_date","2013-02-11")
			    .setParameter("orderRequest.from_station_telecode", "SZQ")
			    .setParameter("orderRequest.to_station_telecode", "CSQ")
			    .setParameter("orderRequest.train_no", "")
			    .setParameter("trainPassType","QB")
			    .setParameter("trainClass", "D#")
			    .setParameter("includeStudent","00")
			    .setParameter("seatTypeAndNum","")
			    .setParameter("orderRequest.start_time_str","00:00--24:00");
			URI uri = builder.build();
			String searchRefer = "https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init";
			HttpGet httpGet = getHttpGet(uri,searchRefer);
			HttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				printlnResponseData(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
				File file = new File("/Users/wenbo/work/yanzhengma/rangCode.jpg");
				byte[] b = new byte[1024];
				outputStream = new BufferedOutputStream(new FileOutputStream(file));
				while(inputStream.read(b) != -1){
					outputStream.write(b);
				}
				outputStream.flush();
			}else{
				Thread.sleep(1000);
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

