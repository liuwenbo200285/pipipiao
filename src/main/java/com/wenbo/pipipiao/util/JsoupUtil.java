package com.wenbo.pipipiao.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsoupUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(JsoupUtil.class);

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws Exception {
//		InputStream inputStream = new FileInputStream(new File("/Users/wenbo/Desktop/1230602"));
//		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"GBK"));
//		String str = "0,<span id='id_6c000G601108' class='base_txtdiv' onmouseover=javascript:onStopHover('6c000G601108#CWQ#IOQ') onmouseout='onStopOut()'>G6011</span>,<img src='/otsweb/images/tips/first.gif'>&nbsp;&nbsp;&nbsp;&nbsp;长沙南&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;07:00,<img src='/otsweb/images/tips/last.gif'>&nbsp;&nbsp;&nbsp;&nbsp;深圳北&nbsp;&nbsp;&nbsp;&nbsp;<br>&nbsp;&nbsp;&nbsp;&nbsp;10:20,03:20,--,<font color='darkgray'>无</font>,<font color='darkgray'>无</font>,4,--,--,--,--,--,--,--,<a name='btn130_2' class='btn130_2' style='text-decoration:none;' onclick=javascript:getSelected('G6011#03:20#07:00#6c000G601108#CWQ#IOQ#10:20#长沙南#深圳北#01#07#O*****0005M*****0000P*****0000#35EB8F56585E40AB3F341A1C908FA836604716C818826A3EF5E0601F#Q6')>预&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;订</a>";
//		IOUtils.closeQuietly(bufferedReader);
//		IOUtils.closeQuietly(inputStream);
	}
	
	
	
	/**
	 * 获取页面对象
	 * @param url
	 * @return
	 */
	public static  Document getPageDocument(InputStream inputStream) {
		Document doc = null;
		try {
			doc = Jsoup.parse(inputStream,"UTF-8","https://dynamic.12306.cn/");
		} catch (Exception e) {
			logger.error("getPageDocument error!",e);
		}finally{
			IOUtils.closeQuietly(inputStream);
		}
		return doc;
	}
	
	
	/**
	 * 获取订票信息
	 * @param document
	 */
	public static String [] getTicketInfo(Document document){
		String str = document.getElementsByTag("a").attr("onclick");
		int begin = StringUtils.indexOf(str,"'");
		int end = StringUtils.lastIndexOf(str,"'");
		str = StringUtils.substring(str, begin+1, end);
		logger.info(str);
		String [] params = StringUtils.split(str,"#");
		return params;
	}
	
	
	/**
	 * 检测有没有票
	 */
	public static int checkHaveTicket(Document document,String type){
		int max = 10000000;
		Integer maxType = 0;
		try {
			if(document == null){
				throw new IllegalAccessException("document is null");
			}
			Elements elements = document.getElementsByTag("span");
			String trainNo = elements.get(0).childNode(0).toString();
			List<Node> nodes = document.childNode(0).childNodes().get(1).childNodes();
			Node node = null;
			int n = 1;
			int index = 0;
			for(int i = 5; i < nodes.size(); i++){
				node = nodes.get(i);
			    if(StringUtils.contains(node.toString(),"--")){
			    	String[] nos = StringUtils.split(node.toString(),",");
			    	if(nos != null && nos.length > 0){
			    		for(String nn:nos){
			    			if("--".equals(nn)){
			    				n++;
			    			}else if(StringUtils.isNumeric(nn)){
			    				if((index = StringUtils.indexOf(type, n+",")) != -1){
			    					logger.info(trainNo+"有票:"+nn+"张!");
			    					max = compare(max,index);
			    					if(max == 0){
			    						return n;
			    					}else if(max == index){
			    						maxType = index;
			    					}
			    				}
			    				n++;
			    			}
			    		}
			    	}
			    }else if("darkgray".equals(node.attr("color"))){
			    	logger.info(trainNo+"没有票!");
			    	n++;
			    }else if("#008800".equals(node.attr("color"))){
			    	if((index = StringUtils.indexOf(type, n+",")) != -1){
			    		logger.info(trainNo+"有大量的票!");
			    		max = compare(max,index);
			    		if(max == 0){
    						return n;
    					}else if(max == index){
    						maxType = index;
    					}
			    	}
			    	n++;
			    }else if(node.hasAttr("onclick")){
					String info = node.childNode(0).toString();
					logger.info(trainNo+info);
//					int bengin = StringUtils.indexOf(info,"点起售");
//					if(bengin != -1){
//						String clo = StringUtils.substring(info,0,bengin);
//						if(StringUtils.isNumeric(clo)){
//							int hour = Integer.valueOf(clo);
////							SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd HH:MM:ss");
////							Date beginDate = simpleDateFormat.parse("2012-01-25 "+hour+":00:00");
////							Date currentDate = new Date();
////							long time = beginDate.getTime()-currentDate.getTime();
//							System.out.println("休息"+time+"毫秒！");
////							Thread.sleep(time);
//						}
//					}
			    }
			}
		} catch (Exception e) {
			logger.error("checkHaveTicket error!",e);
		}
		return maxType;
	}
	
	
	/**
	 * 比较坐席位置，放在前面的优先
	 * @param max
	 * @param index
	 * @return
	 */
	private static int compare(int max,int index){
		if(max == 0
				|| max > index){
			max = index;
		}
		return max;
	}
	
	/**
	 * 检测登录状态
	 * @param inputStream
	 */
	public static boolean validateLogin(Document document){
		Element element = document.getElementById("randErr");
		if(element != null){
			String errorString = element.child(1).childNode(0).toString();
			logger.info("登录失败!原因："+errorString);
		}
		element = document.getElementById("bookTicket");
		if(element != null){
			logger.info("登录成功!");
			return true;
		}
		Elements elements = document.getElementsByAttributeValue("language","javascript");
		if(elements.size() > 0){
			String errorMessage = elements.get(0).childNode(0).toString();
			int i = errorMessage.indexOf("\"");
			int n = errorMessage.indexOf(";");
			logger.info("登录失败!原因："+StringUtils.substring(errorMessage,i+1,n-1));
		}
		return false;
	}

}
