package com.wenbo.httpclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupUtil {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws Exception {
//		InputStream inputStream = new FileInputStream(new File("C:/Users/Administrator/Desktop/1230602.txt"));
//		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"GBK"));
//		String str = bufferedReader.readLine();
		String str = "0,<span id='id_6c000G601108' class='base_txtdiv' onmouseover=javascript:onStopHover('6c000G601108#CWQ#IOQ') onmouseout='onStopOut()'>G6011</span>,<img src='/otsweb/images/tips/first.gif'>&nbsp;&nbsp;&nbsp;&nbsp;长沙南&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;07:00,<img src='/otsweb/images/tips/last.gif'>&nbsp;&nbsp;&nbsp;&nbsp;深圳北&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;10:20,03:20,--,<font color='darkgray'>无</font>,<font color='#008800'>有</font>,<font color='#008800'>有</font>,--,--,--,--,--,--,--,<a name='btn130_2' class='btn130_2' style='text-decoration:none;' onclick=javascript:getSelected('G6011#03:20#07:00#6c000G601108#CWQ#IOQ#10:20#长沙南#深圳北#01#07#O*****0182M*****0024P*****0000#8157002F03D0D1619486047FEDA8D301ABD62F601F27FAC569ECD532#Q6')>预&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;订</a>";
		Document document = Jsoup.parse(str);
		getTicketInfo(document);
	}
	
	
	public static Document turnHtmlToDocument(InputStream inputStream) throws IOException{
		return Jsoup.parse(inputStream,"UTF-8","https://dynamic.12306.cn/");
	}
	
	
	/**
	 * 获取订票信息
	 * @param document
	 */
	public static String [] getTicketInfo(Document document){
		String str = document.getElementsByTag("a").attr("onclick");
		System.out.println(str);
		int begin = StringUtils.indexOf(str,"'");
		int end = StringUtils.lastIndexOf(str,"'");
		str = StringUtils.substring(str, begin+1, end);
		String [] params = StringUtils.split(str,"#");
		for(String para:params){
			System.out.println(para);
		}
		return params;
	}
	
	
	/**
	 * 检测有没有票
	 */
	public static boolean checkHaveTicket(Document document){
		boolean flag = false;
		try {
			if(document == null){
				throw new IllegalAccessException("document is null");
			}
			String str = document.getElementsByTag("font").attr("color");
			if("#008800".equals(str)){
				System.out.println("有票");
				flag = true;
			}else if("darkgray".equals(str)){
				System.out.println("无票");
			}else{
//				String info = document.getElementsByTag("a").get(0).childNode(0).toString();
//				System.out.println(info);
//				int i = StringUtils.indexOf(info,"点起售");
//				if(i != -1){
//					String clo = StringUtils.substring(info,0,i);
//					if(StringUtils.isNumeric(clo)){
//						int hour = Integer.valueOf(clo);
//						SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd HH:MM:ss");
//						Date beginDate = simpleDateFormat.parse("2012-01-25 "+hour+":00:00");
//						Date currentDate = new Date();
//						long time = beginDate.getTime()-currentDate.getTime();
//						System.out.println("休息"+time+"毫秒！");
//						Thread.sleep(time);
//					}
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}
	
	/**
	 * 检测登录状态
	 * @param inputStream
	 */
	public static boolean validateLogin(InputStream inputStream){
		Document document = Demo.getPageDocument(inputStream);
		Element element = document.getElementById("randErr");
		if(element != null){
			String errorString = element.child(1).childNode(0).toString();
			System.out.print("登录失败!原因："+errorString);
		}
		element = document.getElementById("bookTicket");
		if(element != null){
			System.out.println("登录成功!");
			return true;
		}
		Elements elements = document.getElementsByAttributeValue("language","javascript");
		if(elements.size() > 0){
			String errorMessage = elements.get(0).childNode(0).toString();
			int i = errorMessage.indexOf("\"");
			int n = errorMessage.indexOf(";");
			System.out.println("登录失败!原因："+StringUtils.substring(errorMessage,i+1,n-1));
		}
		return false;
	}

}
