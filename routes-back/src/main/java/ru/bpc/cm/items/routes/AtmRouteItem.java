package ru.bpc.cm.items.routes;

import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.utils.CmUtils;

import java.util.Date;

public class AtmRouteItem {
	public AtmRouteItem(int n, int region, Date fdate, boolean changed, int points, int length, int time, long cost, String costCurr, int status, int resultFlag) {
	    super();
	    this.n = n;
	    this.region = region;
	    this.fdate = fdate;
	    this.changed = changed;
	    this.points = points;
	    this.length = length;
	    this.time = time;
	    this.cost = cost;
	    this.costCurr = costCurr;
	    this.status = CmUtils.getEnumValueById(RouteStatus.class, status);
	    this.resultFlag = resultFlag;
    }
	private int n;

	@Override
	public String toString() {
		return "AtmRouteItem{" +
				"n=" + n +
				", region=" + region +
				", fdate=" + fdate +
				", changed=" + changed +
				", points=" + points +
				", length=" + length +
				", time=" + time +
				", cost=" + cost +
				", costCurr='" + costCurr + '\'' +
				", status=" + status +
				", resultFlag=" + resultFlag +
				'}';
	}

	private int region;
	private Date fdate;
	private boolean changed;
	private int points;
	private int length;
	public int time;
	public long cost;
	public String costCurr;
	private RouteStatus status;
	private int resultFlag;
	
	public AtmRouteItem() {
	}
	
	public String getCostCurr() {
		return costCurr;
	}
	public void setCostCurr(String costCurr) {
		this.costCurr = costCurr;
	}
	public long getCost() {
		return cost;
	}
	public void setCost(long cost) {
		this.cost = cost;
	}
	public int getN() {
    	return n;
    }
	public void setN(int n) {
    	this.n = n;
    }
	public int getRegion() {
    	return region;
    }
	public void setRegion(int region) {
    	this.region = region;
    }
	public Date getFdate() {
    	return fdate;
    }
	public void setFdate(Date fdate) {
    	this.fdate = fdate;
    }
	public boolean isChanged() {
    	return changed;
    }
	public void setChanged(boolean changed) {
    	this.changed = changed;
    }
	public int getPoints() {
    	return points;
    }
	public void setPoints(int points) {
    	this.points = points;
    }
	public int getLength() {
    	return length;
    }
	public void setLength(int length) {
    	this.length = length;
    }
	public int getTime() {
    	return time;
    }
	public void setTime(int time) {
    	this.time = time;
    }
	public RouteStatus getStatus() {
		return status;
	}
	public void setStatus(RouteStatus status) {
		this.status = status;
	}
	public int getResultFlag() {
		return resultFlag;
	}
	public void setResultFlag(int resultFlag) {
		this.resultFlag = resultFlag;
	}

	public boolean isEditable(){
		return this.status == null ? false : this.status != RouteStatus.FINISHED;
	}
	
	public boolean isApproveable(){
		return this.status == null ? false : this.status != RouteStatus.FINISHED && this.status != RouteStatus.APPROVED;
	}
	
}
