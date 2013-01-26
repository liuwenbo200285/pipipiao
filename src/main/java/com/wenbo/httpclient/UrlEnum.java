package com.wenbo.httpclient;

public enum UrlEnum {
	//登录url
	LOGIN_INIT_URL("/otsweb/loginAction.do?method=loginAysnSuggest","","","",""),
	//登录验证码
	LOGIN_RANGCODE_URL("/otsweb/passCodeAction.do?rand=sjrand","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8","","",""),
	//登录url
	LONGIN_CONFIM("/otsweb/loginAction.do","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"https://dynamic.12306.cn/otsweb/loginAction.do?method=init",
			"application/x-www-form-urlencoded",""),
	//查询火车票
	SEARCH_TICKET("/otsweb/order/querySingleAction.do","","https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init","",""),
	//点击预定
	BOOK_TICKET("/otsweb/order/querySingleAction.do","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"https://dynamic.12306.cn/otsweb/order/querySingleAction.do?method=init","application/x-www-form-urlencoded",""),
	//确认订单验证码
	ORDER_RANGCODE_URL("/otsweb/passCodeAction.do?rand=randp","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8","","",""),
	//获取坐席值的url
	GET_SEAT_VALUE("/otsweb/passCodeAction.do?rand=randp","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8","","","");
	private String path;
	
	private String accept;
	
	private String refer;
	
	private String contentType;
	
	private String xRequestWith;

	private UrlEnum(String path,String accept,String refer,String contentType,
			String xRequestWith){
		this.path = path;
		this.accept = accept;
		this.refer = refer;
		this.contentType = contentType;
		this.xRequestWith = xRequestWith;
	}

	public String getValue() {
		return path;
	}

	public String getPath() {
		return path;
	}

	public String getRefer() {
		return refer;
	}

	public String getContentType() {
		return contentType;
	}

	public String getxRequestWith() {
		return xRequestWith;
	}

	public String getAccept() {
		return accept;
	}
	
}
