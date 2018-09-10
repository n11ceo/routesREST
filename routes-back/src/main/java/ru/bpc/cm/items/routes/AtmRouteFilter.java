package ru.bpc.cm.items.routes;

import java.io.Serializable;
import java.util.Date;

public class AtmRouteFilter implements Serializable {


    private Date dateStart;
    private int region;
    private int maxPoints;
    private int maxCars;
    private Date newDate;
    
    private int routingType;

    private int optimizationType;

    private static final long serialVersionUID = 1L;

    public AtmRouteFilter() {

    }

    public AtmRouteFilter(Date dateStart, int region, int maxPoints, int maxCars, Date newDate, int routingType, int optimizationType) {
        this.dateStart = dateStart;
        this.region = region;
        this.maxPoints = maxPoints;
        this.maxCars = maxCars;
        this.newDate = newDate;
        this.routingType = routingType;
        this.optimizationType = optimizationType;
    }

    public void setDateStart(Date dateStart) {
	    this.dateStart = dateStart;
    }
	public Date getDateStart() {
	    return dateStart;
    }
	public void setRegion(int region) {
		this.region = region;
	}
	public int getRegion() {
		return region;
	}

	public void setMaxCars(int maxCars) {
	    this.maxCars = maxCars;
    }
	public int getMaxCars() {
	    return maxCars;
    }
	public void setMaxPoints(int maxPoints) {
	    this.maxPoints = maxPoints;
    }
	public int getMaxPoints() {
	    return maxPoints;
    }
	public Date getNewDate() {
		return newDate;
	}
	public void setNewDate(Date newDate) {
		this.newDate = newDate;
	}
	public int getRoutingType() {
		return routingType;
	}
	public int getOptimizationType() {
		return optimizationType;
	}
	public void setRoutingType(int routingType) {
		this.routingType = routingType;
	}
	public void setOptimizationType(int optimizationType) {
		this.optimizationType = optimizationType;
	}



}
