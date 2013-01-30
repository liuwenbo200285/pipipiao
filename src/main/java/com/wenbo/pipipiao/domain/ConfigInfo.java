package com.wenbo.pipipiao.domain;

/**
 * 预定配置VO
 * @author wenbo
 *
 */
public class ConfigInfo {
	private String username;
	
	private String userpass;
	
	private String orderDate;
	
	private String fromStation;
	
	private String toStation;
	
	private String trainNo;
	
	private String trainClass;
	
	private String orderPerson;
	
	private Integer orderSeat;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserpass() {
		return userpass;
	}

	public void setUserpass(String userpass) {
		this.userpass = userpass;
	}

	public String getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}

	public String getFromStation() {
		return fromStation;
	}

	public void setFromStation(String fromStation) {
		this.fromStation = fromStation;
	}

	public String getToStation() {
		return toStation;
	}

	public void setToStation(String toStation) {
		this.toStation = toStation;
	}

	public String getTrainNo() {
		return trainNo;
	}

	public void setTrainNo(String trainNo) {
		this.trainNo = trainNo;
	}

	public String getTrainClass() {
		return trainClass;
	}

	public void setTrainClass(String trainClass) {
		this.trainClass = trainClass;
	}

	public String getOrderPerson() {
		return orderPerson;
	}

	public void setOrderPerson(String orderPerson) {
		this.orderPerson = orderPerson;
	}

	public Integer getOrderSeat() {
		return orderSeat;
	}

	public void setOrderSeat(Integer orderSeat) {
		this.orderSeat = orderSeat;
	}
}
