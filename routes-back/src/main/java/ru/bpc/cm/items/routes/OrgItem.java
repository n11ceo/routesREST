package ru.bpc.cm.items.routes;

public class OrgItem {

	private Integer id;
    private String name;
    private String descx;
    
    private String instId;
    private String instDescx;
    
    private String depotId;
    private String depotDescx;
    
    public OrgItem() {
	}
    
    public OrgItem(OrgItem src) {
		this.id = src.getId();
		this.name = src.getName();
		this.descx = src.getDescx();
		this.instId = src.getInstId();
		this.instDescx = src.getInstDescx();
		this.depotId = src.getDepotId();
		this.depotDescx = src.getDepotDescx();
	}
    
    
	public OrgItem(Integer id, String name, String descx, String instId,
			String depotId, String depotDescx, String instDescx) {
		this.id = id;
		this.name = name;
		this.descx = descx;
		this.instId = instId;
		this.depotId = depotId;
		this.depotDescx = depotDescx;
		this.instDescx = instDescx;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescx() {
		return descx;
	}
	public void setDescx(String descx) {
		this.descx = descx;
	}
	public String getInstId() {
		return instId;
	}
	public void setInstId(String instId) {
		this.instId = instId;
	}
	public String getDepotId() {
		return depotId;
	}
	public void setDepotId(String depotId) {
		this.depotId = depotId;
	}
	public String getDepotDescx() {
		return depotDescx;
	}
	public void setDepotDescx(String depotDescx) {
		this.depotDescx = depotDescx;
	}

	public String getInstDescx() {
		return instDescx;
	}

	public void setInstDescx(String instDescx) {
		this.instDescx = instDescx;
	}
}
