package ru.bpc.cm.items.routes;

public class DepotItem {
	
	private Integer id;
	private String instId;
	private String instDescx;
	private String state;
	private String city;
	private String street;
	private String longitude;
	private String latitude;
	private boolean coordsSet;
	
	public DepotItem() {
	}
	
	public DepotItem(DepotItem src) {
		this.id = src.getId();
		this.instId = src.getInstId();
		this.state = src.getState();
		this.city = src.getCity();
		this.street = src.getStreet();
		this.longitude = src.getLongitude();
		this.latitude = src.getLatitude();
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getInstId() {
		return instId;
	}
	public void setInstId(String instId) {
		this.instId = instId;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getInstDescx() {
		return instDescx;
	}

	public void setInstDescx(String instDescx) {
		this.instDescx = instDescx;
	}

	public boolean isCoordsSet() {
		return coordsSet;
	}

	public void setCoordsSet(boolean coordsSet) {
		this.coordsSet = coordsSet;
	}
	
	
	
}
