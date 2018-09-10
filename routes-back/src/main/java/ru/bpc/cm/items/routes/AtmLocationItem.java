package ru.bpc.cm.items.routes;

import java.io.Serializable;


public class AtmLocationItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * @uml.property  name="pid"
	 */
	private String pid;
	/**
	 * @uml.property  name="city"
	 */
	private String city;
	/**
	 * @uml.property  name="street"
	 */
	private String street;
	/**
	 * @uml.property  name="checked"
	 */
	private boolean checked;
	
	
	/**
	 * @param pid
	 * @uml.property  name="pid"
	 */
	public void setPid(String pid) {
		this.pid = pid;
	}
	/**
	 * @return
	 * @uml.property  name="pid"
	 */
	public String getPid() {
		return pid;
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
	public boolean getChecked() {
		return checked;
	}
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
		

}
