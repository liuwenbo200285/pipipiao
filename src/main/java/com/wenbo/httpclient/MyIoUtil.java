package com.wenbo.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;

public class MyIoUtil {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
//		String str = "asdfasdfasdfasdfasdfasdfasdf";
//        System.out.println(Integer.toHexString(str.getBytes().length));
		InputStream inputStream = new FileInputStream(new File("/Users/wenbo/Desktop/1230606.txt"));
		OutputStream outputStream = new FileOutputStream(new File("/Users/wenbo/Desktop/1230607.txt"));
		MyIoUtil.rendData(inputStream, outputStream);
		IOUtils.closeQuietly(inputStream);
		IOUtils.closeQuietly(outputStream);
		HttpURLConnection httpURLConnection = null;
	}
	
	/**
	 * 
	 */
	public static void rendData(InputStream inputStream,OutputStream outputStream){
		byte[] b = new byte[1];
		try {
			inputStream.read(b);
			String str = byte2hex(b);
			System.out.println(str);
			int size = Integer.parseInt(str,16);
			//忽略2个字节
			b = new byte[2];
			inputStream.read(b);
			System.out.println(size);
			//读取正文
			b = new byte[size];
			inputStream.read(b);
			outputStream.write(b);
			outputStream.flush();
			//忽略2个结束字节
			inputStream.read();
			inputStream.read();
//			rendData(inputStream, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** 
     * java字节码转字符串 
     * @param b 
     * @return 
     */
    public static String byte2hex(byte[] b) { //一个字节的数，

        // 转成16进制字符串
        String hs = "";
        String tmp = "";
        for (int n = 0; n < b.length; n++) {
            //整数转成十六进制表示
            tmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (tmp.length() == 1) {
                hs = hs + "0" + tmp;
            } else {
                hs = hs + tmp;
            }
        }
        tmp = null;
        return hs.toUpperCase(); //转成大写

    }

}
