package com.wenbo.pipipiao.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.wenbo.pipipiao.domain.ConfigInfo;

public class ConfigUtil {

	/**
	 * @param args
	 */
	public static ConfigInfo loadConfigInfo() {
		InputStream inputStream = null;
		ConfigInfo configInfo = null;
		try {
			Properties properties = new Properties();
			inputStream = ConfigUtil.class.getResourceAsStream("/config.properties");
			properties.load(inputStream);
			configInfo = new ConfigInfo();
			configInfo.setUsername(properties.getProperty("username",""));
			configInfo.setUserpass(properties.getProperty("userpass",""));
			configInfo.setOrderDate(properties.getProperty("orderDate",""));
			configInfo.setFromStation(properties.getProperty("fromStation",""));
			configInfo.setToStation(properties.getProperty("toStation",""));
			configInfo.setTrainNo(properties.getProperty("trainNo",""));
			configInfo.setTrainClass(properties.getProperty("trainClass",""));
			configInfo.setOrderPerson(properties.getProperty("orderPerson",""));
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(inputStream);
		}
		return configInfo;
	}

}
