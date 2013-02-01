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
import com.wenbo.pipipiao.util.JsoupUtil;

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
			logger.error("httpclient error!",exception);
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
		new RobTicket(configInfo, userInfo, userInfoMap, httpClient).getLoginRand();
		logger.info("程序执行完毕~~~~~~~~~~~~");
	}	
	
}

