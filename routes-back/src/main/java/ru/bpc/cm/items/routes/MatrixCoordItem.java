package ru.bpc.cm.items.routes;

import java.io.Serializable;

public class MatrixCoordItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * @uml.property  name="pidA"
	 */
	private String pidA;
	/**
	 * @uml.property  name="pidB"
	 */
	private String pidB;
	/**
	 * @uml.property  name="addressA"
	 */
	private String addressA;
	/**
	 * @uml.property  name="addressB"
	 */
	private String addressB;
	/**
	 * @uml.property  name="cityA"
	 */
	private String cityA;
	/**
	 * @uml.property  name="cityB"
	 */
	private String cityB;
	/**
	 * @uml.property  name="coordA"
	 */
	private String coordA;
	/**
	 * @uml.property  name="coordB"
	 */
	private String coordB;
	/**
	 * @uml.property  name="additionalPoints"
	 */
	private String additionalPoints;
	/**
	 * @uml.property  name="distance"
	 */
	private String distance;
	/**
	 * @uml.property  name="time"
	 */
	private String time;
	/**
	 * @uml.property  name="groupid"
	 */
	private String groupid;
	/**
	 * @uml.property  name="needCalc"
	 */
	private boolean needCalc;
	
	/**
	 * @param pidA
	 * @uml.property  name="pidA"
	 */
	public void setPidA(String pidA) {
		this.pidA = pidA;
	}
	/**
	 * @return
	 * @uml.property  name="pidA"
	 */
	public String getPidA() {
		return pidA;
	}
	/**
	 * @param pidB
	 * @uml.property  name="pidB"
	 */
	public void setPidB(String pidB) {
		this.pidB = pidB;
	}
	/**
	 * @return
	 * @uml.property  name="pidB"
	 */
	public String getPidB() {
		return pidB;
	}
	/**
	 * @param addressA
	 * @uml.property  name="addressA"
	 */
	public void setAddressA(String addressA) {
		this.addressA = addressA;
	}
	/**
	 * @return
	 * @uml.property  name="addressA"
	 */
	public String getAddressA() {
		return addressA;
	}
	/**
	 * @param addressB
	 * @uml.property  name="addressB"
	 */
	public void setAddressB(String addressB) {
		this.addressB = addressB;
	}
	/**
	 * @return
	 * @uml.property  name="addressB"
	 */
	public String getAddressB() {
		return addressB;
	}
	/**
	 * @param cityA
	 * @uml.property  name="cityA"
	 */
	public void setCityA(String cityA) {
		this.cityA = cityA;
	}
	/**
	 * @return
	 * @uml.property  name="cityA"
	 */
	public String getCityA() {
		return cityA;
	}
	/**
	 * @param cityB
	 * @uml.property  name="cityB"
	 */
	public void setCityB(String cityB) {
		this.cityB = cityB;
	}
	/**
	 * @return
	 * @uml.property  name="cityB"
	 */
	public String getCityB() {
		return cityB;
	}
	/**
	 * @param coordA
	 * @uml.property  name="coordA"
	 */
	public void setCoordA(String coordA) {
		this.coordA = coordA;
	}
	/**
	 * @return
	 * @uml.property  name="coordA"
	 */
	public String getCoordA() {
		return coordA;
	}
	/**
	 * @param coordB
	 * @uml.property  name="coordB"
	 */
	public void setCoordB(String coordB) {
		this.coordB = coordB;
	}
	/**
	 * @return
	 * @uml.property  name="coordB"
	 */
	public String getCoordB() {
		return coordB;
	}
	/**
	 * @param distance
	 * @uml.property  name="distance"
	 */
	public void setDistance(String distance) {
		this.distance = distance;
	}
	/**
	 * @return
	 * @uml.property  name="distance"
	 */
	public String getDistance() {
		return distance;
	}
	/**
	 * @param additionalPoints
	 * @uml.property  name="additionalPoints"
	 */
	public void setAdditionalPoints(String additionalPoints) {
		this.additionalPoints = additionalPoints;
	}
	/**
	 * @return
	 * @uml.property  name="additionalPoints"
	 */
	public String getAdditionalPoints() {
		return additionalPoints;
	}
	/**
	 * @param time
	 * @uml.property  name="time"
	 */
	public void setTime(String time) {
		this.time = time;
	}
	/**
	 * @return
	 * @uml.property  name="time"
	 */
	public String getTime() {
		return time;
	}
	/**
	 * @param groupid
	 * @uml.property  name="groupid"
	 */
	public void setGroupid(String groupid) {
		this.groupid = groupid;
	}
	/**
	 * @return
	 * @uml.property  name="groupid"
	 */
	public String getGroupid() {
		return groupid;
	}
	/**
	 * @param needCalc
	 * @uml.property  name="needCalc"
	 */
	public void setNeedCalc(boolean needCalc) {
		this.needCalc = needCalc;
	}
	/**
	 * @return
	 * @uml.property  name="needCalc"
	 */
	public boolean isNeedCalc() {
		return needCalc;
	}
	

}
