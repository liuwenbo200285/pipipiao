package com.wenbo.pipipiao.httpclient;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

public class ConnectionManager {
static final int TIMEOUT = 20000;//连接超时时间
static final int SO_TIMEOUT = 60000;//数据传输超时

public static DefaultHttpClient getHttpClient(){
//	SchemeRegistry schemeRegistry = new SchemeRegistry();
//	schemeRegistry.register(
//			new Scheme("http",80,PlainSocketFactory.getSocketFactory()));
//	schemeRegistry.register(
//			new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
	PoolingClientConnectionManager  cm = new PoolingClientConnectionManager();
	cm.setMaxTotal(200);
	cm.setDefaultMaxPerRoute(20);
	HttpParams params = new BasicHttpParams();
	params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,TIMEOUT);
	params.setParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
	DefaultHttpClient client = new DefaultHttpClient(cm,params);
	return client;
}
}

