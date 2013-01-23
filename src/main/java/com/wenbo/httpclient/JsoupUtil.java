package com.wenbo.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupUtil {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		InputStream inputStream = new FileInputStream(new File("/Users/wenbo/work/yanzhengma/passwordError"));
        validateLogin(inputStream);
	}
	
	public static void validateLogin(InputStream inputStream){
		Document document = Demo.getPageDocument(inputStream);
		Element element = document.getElementById("randErr");
		if(element != null){
			String errorString = element.child(1).childNode(0).toString();
			System.out.print("登录失败!原因："+errorString);
		}
		Elements elements = document.getElementsByAttributeValue("language","javascript");
		if(elements.size() > 0){
			String errorMessage = elements.get(0).childNode(0).toString();
			int i = errorMessage.indexOf("\"");
			int n = errorMessage.indexOf(";");
			System.out.println("登录失败!原因："+StringUtils.substring(errorMessage,i+1,n-1));
		}else{
			element = document.getElementById("bookTicket");
			if(element != null){
				System.out.println("登录成功!");
			}
		}
	}

}
