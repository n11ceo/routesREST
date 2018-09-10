package ru.bpc.cm.items.routes;

import java.io.Serializable;

public class AtmRoutePointItem implements Serializable {



	public AtmRoutePointItem(int n, int encID, String pid, String latitude, String longitude, String adress, int arrivalTime, boolean visited, boolean reorder, boolean depot, boolean broken) {
	    super();
	    this.n = n;
	    this.pid = pid;
	    this.setEncID(encID);
	    this.latitude = latitude;
	    this.longitude = longitude;
	    this.adress = adress;
	    this.arrivalTime = arrivalTime;
	    this.visited = visited;
	    this.reorder = reorder;
	    this.depot = depot;
	    this.broken = broken;
    }

	private int n;
    private String pid;
    private int encID;
    private int arrivalTime;
    private String latitude;
    private String longitude;
    private String adress;
    private boolean visited;
    private boolean reorder;
    private boolean depot;
    private boolean broken;

    private static final long serialVersionUID = 1L;

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getPid() {
		return pid;
	}

	public int getEncID() {
		return encID;
	}

	public void setEncID(int encID) {
		this.encID = encID;
	}

	public int getN() {
    	return n;
    }

	public void setN(int n) {
    	this.n = n;
    }

	public String getLatitude() {
    	return latitude;
    }

	public void setLatitude(String latitude) {
    	this.latitude = latitude;
    }

	public String getLongitude() {
    	return longitude;
    }

	public void setLongitude(String longitude) {
    	this.longitude = longitude;
    }

	public void setAdress(String adress) {
	    this.adress = adress;
    }

	public String getAdress() {
	    return adress;
    }

	public int getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(int arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public boolean isReorder() {
		return reorder;
	}

	public void setReorder(boolean reorder) {
		this.reorder = reorder;
	}

	public boolean isDepot() {
		return depot;
	}

	public void setDepot(boolean depot) {
		this.depot = depot;
	}

	public boolean isBroken() {
		return broken;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}





}
