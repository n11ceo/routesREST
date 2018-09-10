package ru.bpc.cm.routes;

import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bpc.cm.cashmanagement.CmCommonController;
import ru.bpc.cm.items.enums.OrgAttribute;
import ru.bpc.cm.items.enums.RouteLogType;
import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.items.routes.*;
import ru.bpc.cm.items.routing.Matrix;
import ru.bpc.cm.items.routing.SolutionRoutes;
import ru.bpc.cm.items.routing.VNS.InitialSol.MySol;
import ru.bpc.cm.items.routing.antmethod.Common.Place;
import ru.bpc.cm.items.routing.heneticmethod.Riders;
import ru.bpc.cm.items.settings.AtmGroupAttributeItem;
import ru.bpc.cm.utils.db.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RoutingUtils {
	
	private static final Logger logger = LoggerFactory.getLogger("CASH_MANAGEMENT");

	public static void getDistanceMatrixForATMs(List<MatrixCoordItem> list, AtomicInteger progress){
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyDa83DzRLUuNWYEtNmc5paFlLAmrCtlgPU");   //AIzaSyDUdRxUd-4YMeYtBENfgcMiiU9ciJyOMHU
		String[] coords=null;
		DistanceMatrix result;

		progress.set(0);

		int count=0;

		for (MatrixCoordItem item : list){
			count++;
			DistanceMatrixApiRequest req = new DistanceMatrixApiRequest(context);
			if (item.getCoordA()=="_" || item.getCoordB()=="_" || !item.isNeedCalc()){
				return;
			} else  {
				coords=item.getCoordA().split(",");
				if (coords.length==2){
					req.origins(new LatLng(Float.parseFloat(coords[0]), Float.parseFloat(coords[1])));
				}
				coords=item.getCoordB().split(",");
				if (coords.length==2){
					req.destinations(new LatLng(Float.parseFloat(coords[0]), Float.parseFloat(coords[1])));
				}

				try {
					result = req.await();
					for (DistanceMatrixRow row : result.rows){
						for(DistanceMatrixElement element : row.elements){
							item.setDistance(String.valueOf(element.distance.inMeters));
							item.setTime(String.valueOf(element.duration.inSeconds));
							System.out.println("Distance: "+element.distance.inMeters+" m.");
							System.out.println("Time: "+element.duration.inSeconds+" sec.");
						}
					}
					progress.set(Math.round(count*100/list.size()));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}
	


	public static void updateRouteStatus(Connection con, int routeN, RouteStatus status){//, boolean singleRouteRefresh TODO: statuses handling for single route update
	    PreparedStatement updatestmt = null;
	    
	    String updateQuery ="Update T_CM_ROUTE SET ROUTE_STATUS = ? where ID = ? ";
		try {
			updatestmt = con.prepareStatement(updateQuery);
			updatestmt.setInt(1, status.getId());
			updatestmt.setInt(2, routeN);
			updatestmt.executeUpdate();
			
			if(status == RouteStatus.APPROVED){
				RoutingController.insertRouteLog(con, routeN, RouteLogType.ROUTE_APPROVED, String.valueOf(routeN));
			}
		} catch (SQLException e) {
			logger.error("Routes result update ERROR",e);
		} finally {
			JdbcUtils.close(updatestmt);
		}
	}
	
	protected static void updateRoutesCost(Connection con, AtmRouteFilter filter, Riders result){
		int serviceTime=0;
		int currencyCode=840;
		long routeCost=0;
		double kilometer_cost=0;
		double fix_price=0;
		List<AtmGroupAttributeItem> attrList = OrgController.getAttributeListForOrg(con, filter.getRegion());
		for (AtmGroupAttributeItem item: attrList){
			if (item.isUsed()){
				if (item.getAttributeID()==OrgAttribute.TECHNICAL_TIME.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							serviceTime+=Integer.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.KILOMETER_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							kilometer_cost+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.FIX_ENC_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							fix_price+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.CURRENCY_CODE.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							currencyCode=Integer.valueOf(item.getValue());
						}
				}
			}
		}
		PreparedStatement prep = null;
		PreparedStatement prepInfo = null;
		PreparedStatement prepUpdate = null;
		ResultSet rs = null;
		ResultSet rsInfo = null;
		
		ArrayList<AtmRouteItem> routeList = new ArrayList<AtmRouteItem>();
		String query = "select ID, ROUTE_DATE, ORG_ID, RESULT_FLAG, atmp.POINTS "+
						"from T_CM_ROUTE left outer join (select count(ORD) as POINTS, ROUTE_ID from T_CM_ROUTE_POINT group by ROUTE_ID) atmp on atmp.ROUTE_ID=T_CM_ROUTE.ID "+
						"where ORG_ID=? and ROUTE_DATE=? " +
						"ORDER BY ID ASC";
		String queryInfo = "select sum(distance) as distance, sum(time) as time, count(distance) as atmNum  from T_CM_ROUTE_NODES arn "+
				        "join (select ap1.ATM_ID as pida, ap2.ATM_ID as pidb "+
				        "from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap1 "+
				        "join (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap2 on (ap1.ord = ap2.ord-1 and ap1.route_id = ap2.route_id) "+
				        "where ap1.route_id = ? "+
				        "union select * from (select arnd.pida, arnd.pidb from T_CM_ROUTE_NODES arnd,T_CM_ROUTE route,  T_CM_ROUTE_ORG org "+
				        "where route.id=? and route.org_id=org.id and arnd.pida=org.depot_id and arnd.pidb in (select ATM_ID from  "+
				        "( select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID " +
				          "and route_id = ?"+
				          "order by ORD asc ) "+
				        " "+JdbcUtils.getLimitToFirstRowExpression(con)+")) u1 "+
//				        "union select * from (select arnd.pida, arnd.pidb from T_CM_ROUTE_NODES arnd,T_CM_ROUTE route,  T_CM_ROUTE_ORG org "+
//				        "where route.id=? and route.org_id=org.id and arnd.pidb=org.depot_id and arnd.pida in (select POINT_SRC_ID from  "+
//				        "( select * "+
//				          "from T_CM_ROUTE_POINT "+
//				          "where route_id = ?"+
//				          "order by ORD desc ) "+
//				        " "+JdbcUtils.getLimitToFirstRowExpression(con)+")) u2 "+
				        ") x on (arn.pida = x.pida and arn.pidb = x.pidb) ";
		String update ="Update T_CM_ROUTE SET COST=?, COST_CURR=?, ROUTE_TIME=?, ROUTE_LENGTH=? where   ID = ?";
		try {
	        prep = con.prepareStatement(query);
	        prep.setInt(1, filter.getRegion());
	        prep.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
	        rs = prep.executeQuery();
	        int length=0;
	        //int time=0;
	        //int riderNumber=0;
	        prepInfo = con.prepareStatement(queryInfo);
	        while (rs.next()){
		        prepInfo.setInt(1, rs.getInt("ID"));
		        prepInfo.setInt(2, rs.getInt("ID"));
		        prepInfo.setInt(3, rs.getInt("ID"));
		        //prepInfo.setInt(4, rs.getInt("ID"));
		        //prepInfo.setInt(5, rs.getInt("ID"));
		        rsInfo = prepInfo.executeQuery();
		        if (rsInfo.next()){
		        	length = rsInfo.getInt("distance");
		        	//time = rsInfo.getInt("time");
		        	routeCost=Math.round(length*kilometer_cost/1000+fix_price*(rsInfo.getInt("atmNum")));//rsInfo.getInt("atmNum")-1
		        	routeList.add(
		        			new AtmRouteItem(rs.getInt("ID"), filter.getRegion(), filter.getDateStart(), 
		        					false, rs.getInt("POINTS"), length, 
		        					getRouteTimeFromPoints(con, rs.getInt("ID"), serviceTime),routeCost,"USD",0,rs.getInt("RESULT_FLAG")));//TODO: status always 0  rsInfo.getInt("atmNum")-1
		        	//Math.round(time/60)+serviceTime*(rsInfo.getInt("atmNum"))+ (result.isGood() ? result.getRiderSumWaitTime(riderNumber) : 0)
		        }
		        JdbcUtils.close(rsInfo);
	        	
	        }
	        JdbcUtils.close(rs);
	        JdbcUtils.close(prep);
	        JdbcUtils.close(prepInfo);
	        
	        prepUpdate = con.prepareStatement(update);
	        for (AtmRouteItem item : routeList){
	        	item.setCostCurr(CmCommonController.getCurrCodeA3(con, currencyCode));
	        	prepUpdate.setLong(1, item.getCost());
	        	prepUpdate.setInt(2, currencyCode);
	        	prepUpdate.setInt(3, item.getTime());
	        	prepUpdate.setInt(4, item.getLength());
	        	prepUpdate.setInt(5, item.getN());
	        	prepUpdate.executeUpdate();
	        }
	        JdbcUtils.close(prepUpdate);
	        
        } catch (SQLException e) {
        	logger.error("Cost update ERROR",e);
        }
	}
	
	protected static void updateRoutesCost(Connection con, AtmRouteFilter filter) {
		int serviceTime=0;
		int currencyCode=840;
		long routeCost=0;
		double kilometer_cost=0;
		double fix_price=0;
		List<AtmGroupAttributeItem> attrList = OrgController.getAttributeListForOrg(con, filter.getRegion());
		for (AtmGroupAttributeItem item: attrList){
			if (item.isUsed()){
				if (item.getAttributeID()==OrgAttribute.TECHNICAL_TIME.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							serviceTime+=Integer.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.KILOMETER_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							kilometer_cost+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.FIX_ENC_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							fix_price+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.CURRENCY_CODE.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							currencyCode=Integer.valueOf(item.getValue());
						}
				}
			}
		}
		PreparedStatement prep = null;
		PreparedStatement prepInfo = null;
		PreparedStatement prepUpdate = null;
		ResultSet rs = null;
		ResultSet rsInfo = null;
		
		ArrayList<AtmRouteItem> routeList = new ArrayList<AtmRouteItem>();
		String query = "select ID, ROUTE_DATE, ORG_ID, RESULT_FLAG, atmp.POINTS "+
						"from T_CM_ROUTE left outer join (select count(ORD) as POINTS, ROUTE_ID from T_CM_ROUTE_POINT group by ROUTE_ID) atmp on atmp.ROUTE_ID=T_CM_ROUTE.ID "+
						"where ORG_ID=? and ROUTE_DATE=? ";
		String queryInfo = "select sum(distance) as distance, sum(time) as time, count(distance) as atmNum  from T_CM_ROUTE_NODES arn "+
				        "join (select ap1.ATM_ID as pida, ap2.ATM_ID as pidb "+
				        "from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap1 "+
				        "join (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap2 on (ap1.ord = ap2.ord-1 and ap1.route_id = ap2.route_id) "+
				        "where ap1.route_id = ? "+
				        "union select * from (select arnd.pida, arnd.pidb from T_CM_ROUTE_NODES arnd,T_CM_ROUTE route,  T_CM_ROUTE_ORG org "+
				        "where route.id=? and route.org_id=org.id and arnd.pida=org.depot_id and arnd.pidb in (select ATM_ID from  "+
				        "( select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID " +
				          "and route_id = ?"+
				          "order by ORD asc ) "+
				        " "+JdbcUtils.getLimitToFirstRowExpression(con)+")) u1 "+
//				        "union select * from (select arnd.pida, arnd.pidb from T_CM_ROUTE_NODES arnd,T_CM_ROUTE route,  T_CM_ROUTE_ORG org "+
//				        "where route.id=? and route.org_id=org.id and arnd.pidb=org.depot_id and arnd.pida in (select POINT_SRC_ID from  "+
//				        "( select * "+
//				          "from T_CM_ROUTE_POINT "+
//				          "where route_id = ?"+
//				          "order by ORD desc ) "+
//				        ""+JdbcUtils.getLimitToFirstRowExpression(con)+")) u2 "+
				        ") x on (arn.pida = x.pida and arn.pidb = x.pidb) ";
		String update ="Update T_CM_ROUTE SET COST=?, COST_CURR=?, ROUTE_TIME=?, ROUTE_LENGTH=? where   ID = ?";
		try {
	        prep = con.prepareStatement(query);
	        prep.setInt(1, filter.getRegion());
	        prep.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
	        rs = prep.executeQuery();
	        int length=0;
	        //int time=0;
	        prepInfo = con.prepareStatement(queryInfo);
	        while (rs.next()){
		        prepInfo.setInt(1, rs.getInt("ID"));
		        prepInfo.setInt(2, rs.getInt("ID"));
		        prepInfo.setInt(3, rs.getInt("ID"));
		        //prepInfo.setInt(4, rs.getInt("ID"));
		        //prepInfo.setInt(5, rs.getInt("ID"));
		        rsInfo = prepInfo.executeQuery();
		        if (rsInfo.next()){
		        	length = rsInfo.getInt("distance");
		        	//time = rsInfo.getInt("time");
		        	routeCost=Math.round(length*kilometer_cost/1000+fix_price*(rsInfo.getInt("atmNum")));
		        	routeList.add(
		        			new AtmRouteItem(rs.getInt("ID"), filter.getRegion(), filter.getDateStart(), 
		        					false, rs.getInt("POINTS"), length, 
		        					getRouteTimeFromPoints(con, rs.getInt("ID"), serviceTime),routeCost,"USD",0,rs.getInt("RESULT_FLAG")));//TODO: status always 0
		        	//Math.round(time/60)+serviceTime*(rsInfo.getInt("atmNum"))
		        }
		        JdbcUtils.close(rsInfo);
	        	
	        }
	        JdbcUtils.close(rs);
	        JdbcUtils.close(prep);
	        JdbcUtils.close(prepInfo);
	        
	        prepUpdate = con.prepareStatement(update);
	        for (AtmRouteItem item : routeList){
	        	item.setCostCurr(CmCommonController.getCurrCodeA3(con, currencyCode));
	        	prepUpdate.setLong(1, item.getCost());
	        	prepUpdate.setInt(2, currencyCode);
	        	prepUpdate.setInt(3, item.getTime());
	        	prepUpdate.setInt(4, item.getLength());
	        	prepUpdate.setInt(5, item.getN());
	        	prepUpdate.executeUpdate();
	        }
	        JdbcUtils.close(prepUpdate);
	        
        } catch (SQLException e) {
        	logger.error("Cost update ERROR",e);
        }
	}
	
	protected static void updateDefaultRouteCostAndArrivalTimes(Connection con, AtmRouteFilter filter, Matrix matrix) {
		int serviceTime=0;
		int currencyCode=840;
		long routeCost=0;
		double kilometer_cost=0;
		double fix_price=0;
		List<AtmGroupAttributeItem> attrList = OrgController.getAttributeListForOrg(con, filter.getRegion());
		for (AtmGroupAttributeItem item: attrList){
			if (item.isUsed()){
				if (item.getAttributeID()==OrgAttribute.TECHNICAL_TIME.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							serviceTime+=Integer.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.KILOMETER_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							kilometer_cost+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.FIX_ENC_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Double.valueOf(item.getValue())>0){
							fix_price+=Double.valueOf(item.getValue());
						}
				}
				if (item.getAttributeID()==OrgAttribute.CURRENCY_CODE.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						if (Integer.valueOf(item.getValue())>0){
							currencyCode=Integer.valueOf(item.getValue());
						}
				}
			}
		}
		PreparedStatement prep = null;
		PreparedStatement prepInfo = null;
		PreparedStatement prepUpdate = null;
		ResultSet rs = null;
		ResultSet rsInfo = null;
		
		ArrayList<AtmRouteItem> routeList = new ArrayList<AtmRouteItem>();
		String query = "select ID, ROUTE_DATE, ORG_ID, RESULT_FLAG, atmp.POINTS "+
						"from T_CM_ROUTE left outer join (select count(ORD) as POINTS, ROUTE_ID from T_CM_ROUTE_POINT group by ROUTE_ID) atmp on atmp.ROUTE_ID=T_CM_ROUTE.ID "+
						"where ORG_ID=? and ROUTE_DATE=? ";
		String queryInfo = "select sum(distance) as distance, sum(time) as time, count(distance) as atmNum  from T_CM_ROUTE_NODES arn "+
				        "join (select ap1.ATM_ID as pida, ap2.ATM_ID as pidb "+
				        "from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap1 "+
				        "join (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) ap2 on (ap1.ord = ap2.ord-1 and ap1.route_id = ap2.route_id) "+
				        "where ap1.route_id = ? "+
				        "union select * from (select arnd.pida, arnd.pidb from T_CM_ROUTE_NODES arnd,T_CM_ROUTE route,  T_CM_ROUTE_ORG org "+
				        "where route.id=? and route.org_id=org.id and arnd.pida=org.depot_id and arnd.pidb in (select ATM_ID from  "+
				        "( select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID " +
				          "and route_id = ?"+
				          "order by ORD asc ) "+
				        " "+JdbcUtils.getLimitToFirstRowExpression(con)+")) u1 "+
				        ") x on (arn.pida = x.pida and arn.pidb = x.pidb) ";
		String update ="Update T_CM_ROUTE SET COST=?, COST_CURR=?, ROUTE_TIME=?, ROUTE_LENGTH=? where   ID = ?";
		try {
	        prep = con.prepareStatement(query);
	        prep.setInt(1, filter.getRegion());
	        prep.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
	        rs = prep.executeQuery();
	        int length=0;
	        int time=0;
	        prepInfo = con.prepareStatement(queryInfo);
	        while (rs.next()){
		        prepInfo.setInt(1, rs.getInt("ID"));
		        prepInfo.setInt(2, rs.getInt("ID"));
		        prepInfo.setInt(3, rs.getInt("ID"));
		        //prepInfo.setInt(4, rs.getInt("ID"));
		        //prepInfo.setInt(5, rs.getInt("ID"));
		        rsInfo = prepInfo.executeQuery();
		        if (rsInfo.next()){
		        	length = rsInfo.getInt("distance");
		        	time = rsInfo.getInt("time");
		        	routeCost=Math.round(length*kilometer_cost/1000+fix_price*(rsInfo.getInt("atmNum")));
		        	routeList.add(
		        			new AtmRouteItem(rs.getInt("ID"), filter.getRegion(), filter.getDateStart(), 
		        					false, rs.getInt("POINTS"), length, 
		        					Math.round(time/60)+serviceTime*(rsInfo.getInt("atmNum")),routeCost,"USD",0,rs.getInt("RESULT_FLAG")));//TODO: status always 0
		        }
		        JdbcUtils.close(rsInfo);
	        	
	        }
	        JdbcUtils.close(rs);
	        JdbcUtils.close(prep);
	        JdbcUtils.close(prepInfo);
	        
	        prepUpdate = con.prepareStatement(update);
	        for (AtmRouteItem item : routeList){
	        	item.setCostCurr(CmCommonController.getCurrCodeA3(con, currencyCode));
	        	prepUpdate.setLong(1, item.getCost());
	        	prepUpdate.setInt(2, currencyCode);
	        	prepUpdate.setInt(3, item.getTime());
	        	prepUpdate.setInt(4, item.getLength());
	        	prepUpdate.setInt(5, item.getN());
	        	prepUpdate.executeUpdate();
	        }
	        JdbcUtils.close(prepUpdate);
	        for (AtmRouteItem item : routeList){
	        	calculateArrivalTimes(con, filter, matrix, item.getN());
	        }
	        
        } catch (SQLException e) {
        	logger.error("Default Route cost and arrival times update ERROR",e);
        } finally {
        	
        }
	}
	
	protected static void updateInProgressRoute(Connection con, int routeN, Matrix matrix, Riders result, AtmRouteFilter filter){ 
		RoutingUtils.clearInProgressRoute(con, routeN);
        PreparedStatement pointstmt = null;
        PreparedStatement ordstmt = null;
        ResultSet rs = null;
        String insertPoint ="Insert into T_CM_ROUTE_POINT (ROUTE_ID, TYP, POINT_SRC_ID, ORD, POINT_TIME) Values(?, ?, ?, ?, ?) ";
        String maxord="select max(ord) as maxord from T_CM_ROUTE_POINT where route_id=?";
        try {
        	ordstmt = con.prepareStatement(maxord);
        	ordstmt.setInt(1, routeN);
        	rs = ordstmt.executeQuery();
        	rs.next();
        	int initorder=rs.getInt("maxord");
        	JdbcUtils.close(rs);
        	JdbcUtils.close(ordstmt);
        	
			pointstmt = con.prepareStatement(insertPoint);
			int order=initorder+1;
			int j=1;
			if (result!=null){
				
				for (int i=0; i<result.getRidersCount(); i++){
					if (result.getRiderATM(i).length>0) {
						order = initorder+1;
						j=1;
						for (Integer pid : result.getRiderATM(i)) {
							logger.debug(Integer.toString(pid));
							pointstmt.setInt(1, routeN);
							pointstmt.setInt(2, 1);
							pointstmt.setInt(3, pid);
							pointstmt.setInt(4, order++);
							if (result.riderList.get(i).time.size()!=0){
								pointstmt.setInt(5, result.getRiderTime(i, j++));
							} else {
								pointstmt.setInt(5, 0);
							}
							
							pointstmt.executeUpdate();
							
						}
					}
				}
			}
		} catch (SQLException e) {
			logger.error("Error while Saving IN Progress Route",e);
		} finally {
			JdbcUtils.close(rs);
        	JdbcUtils.close(ordstmt);
			JdbcUtils.close(pointstmt);
		}
	}
	
	protected static void updateRouteResult(Connection con, int routeN, boolean statusFlag, boolean inProgressFlag, boolean singleRouteRefresh, Matrix matrix){
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        String updateQuery ="Update T_CM_ROUTE SET RESULT_FLAG = ? where ID = ? ";
        logger.debug("Updating Result for Route : "+routeN+" result: "+matrix.constraintsStatus);
        //String infoQuery="Select RESULT_FLAG from T_CM_ROUTE where ID = ? ";
		try {
			if (!singleRouteRefresh){
				stmt = con.prepareStatement(updateQuery);
				stmt.setInt(1, matrix.constraintsStatus);//(inProgressFlag ? (statusFlag ? 0 : 1) : (statusFlag ? 2 : 1))
				stmt.setInt(2, routeN);
				stmt.executeUpdate();
			} else {
				/*stmt = con.prepareStatement(infoQuery);
				stmt.setInt(1, routeN);
				rs = stmt.executeQuery();
				rs.next();
				int oldFlag = rs.getInt("RESULT_FLAG");
				JdbcUtils.close(rs);
				JdbcUtils.close(stmt);*/
				stmt = con.prepareStatement(updateQuery);
				stmt.setInt(1, matrix.constraintsStatus);//(oldFlag==1 ? (statusFlag ? 0 : 1) : (statusFlag ? 0 : 1))
				stmt.setInt(2, routeN);
				stmt.executeUpdate();
			}
			
		} catch (SQLException e) {
			logger.error("Routes result update ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(stmt);
		}
	}


	protected static void saveRoutes(Connection con, AtmRouteFilter filter, Matrix matrix, Riders result){
		RoutingUtils.clearRoutes(con, filter);
        PreparedStatement pointstmt = null;
        PreparedStatement routestmt = null;
        ResultSet rs = null;
        int routeN = 0;
        
        
        	
		try {
            String idQuery = "SELECT " + JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE") + " as CNT " +
                    JdbcUtils.getFromDummyExpression(con);
            String insertRoute = "Insert into T_CM_ROUTE (ID, ROUTE_DATE, ORG_ID, ROUTE_STATUS, RESULT_FLAG) Values(" +
                    JdbcUtils.getNextSequence(con, "SQ_CM_ROUTE") + ", ?, ?, 1, ?) ";
            String insertPoint = "Insert into T_CM_ROUTE_POINT (ROUTE_ID, TYP, POINT_SRC_ID, ORD, POINT_TIME, VISITED_FLAG) " +
                    "Values(" + JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE") + ", ?, ?, ?, ?, 0) ";

            int order = 2;
            int j = 1;
            if (result != null) {
                for (int i = 0; i < result.getRidersCount(); i++) {
                    routestmt = con.prepareStatement(insertRoute);
                    routestmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
                    routestmt.setInt(2, filter.getRegion());
                    routestmt.setInt(3, result.isGood() ? 0 : matrix.constraintsStatus);//1
                    
                    System.out.println(routestmt);
                    System.out.println(filter.getDateStart());
                    System.out.println(filter.getRegion());
                    System.out.println(result.isGood() ? 0 : matrix.constraintsStatus);
                    
                    routestmt.executeUpdate();
                    JdbcUtils.close(routestmt);

                    routestmt = con.prepareStatement(idQuery);
                    rs = routestmt.executeQuery();
                    if (rs.next()) {
                        routeN = rs.getInt("CNT");
                    }

                    RoutingController.insertRouteLog(con, routeN, RouteLogType.ROUTE_CREATED, String.valueOf(routeN));

                    if (result.getRiderATM(i).length > 0) {
                        order = 1;
                        j = 0;//j=1
                        
                        
                        for (Integer pid : result.getRiderATM(i)) {
                            pointstmt = con.prepareStatement(insertPoint);
                            pointstmt.setInt(1, 1);
                            pointstmt.setInt(2, pid);
                            pointstmt.setInt(3, order++);
                            pointstmt.setInt(4, result.getRiderTime(i, j++) - matrix.getRiderTimeWindows().get(0).StartWork);//
                            pointstmt.executeUpdate();
                            
                            System.out.println(pointstmt);
                            System.out.println(pid);
                            System.out.println(order-1);
                            System.out.println(result.getRiderTime(i, j-1) - matrix.getRiderTimeWindows().get(0).StartWork);
                            
                            
                            JdbcUtils.close(pointstmt);

                            RoutingController.insertRouteLog(con, routeN, RouteLogType.POINT_ADDED, pid.toString(),
                                    String.valueOf(order - 1));

                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Routes Save ERROR", e);
            return;
        } finally {
            JdbcUtils.close(pointstmt);
            JdbcUtils.close(routestmt);
        }

        RoutingUtils.updateRoutesCost(con, filter, result);
    }

    protected static void saveRoutes(Connection con, AtmRouteFilter filter, Matrix matrix, SolutionRoutes result) {
        RoutingUtils.clearRoutes(con, filter);
        PreparedStatement pointstmt = null;
        PreparedStatement routestmt = null;
        ResultSet rs = null;
        int routeN = 0;


        try {
            String idQuery = "SELECT " + JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE") + " as CNT " +
                    JdbcUtils.getFromDummyExpression(con);
            String insertRoute = "Insert into T_CM_ROUTE (ID, ROUTE_DATE, ORG_ID, ROUTE_STATUS, RESULT_FLAG) Values(" +
                    JdbcUtils.getNextSequence(con, "SQ_CM_ROUTE") + ", ?, ?, 1, ?) ";
            String insertPoint = "Insert into T_CM_ROUTE_POINT (ROUTE_ID, TYP, POINT_SRC_ID, ORD, POINT_TIME, VISITED_FLAG) " +
                    "Values(" + JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE") + ", ?, ?, ?, ?, 0) ";

            if (result != null) {
                for (ArrayList<Integer> route : result.getRoutes()) {
                    routestmt = con.prepareStatement(insertRoute);
                    routestmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
                    routestmt.setInt(2, filter.getRegion());
                    routestmt.setInt(3, result.getStatus().getValue());
                    routestmt.executeUpdate();
                    
                    System.out.println(routestmt);
                    System.out.println(filter.getDateStart());
                    System.out.println(filter.getRegion());
                    System.out.println(1);
                    
                    JdbcUtils.close(routestmt);

                    routestmt = con.prepareStatement(idQuery);
                    rs = routestmt.executeQuery();
                    if (rs.next()) {
                        routeN = rs.getInt("CNT");
                    }

                    RoutingController.insertRouteLog(con, routeN, RouteLogType.ROUTE_CREATED, String.valueOf(routeN));

                    int order = 1;
                    int pointTime = matrix.getTimeWindows().get(0).StartWork; // start time of depot
                    pointTime -= matrix.serviceTime[0]; // if not serving depot in the beginning
                    int prevCust = 0;
                    for (Integer pid : route) {
                        Integer EncPid = matrix.ENC[pid];
                        pointTime = Math.max(pointTime + matrix.timeCoeffs[prevCust][pid] + matrix.serviceTime[prevCust], matrix.getTimeWindows().get(pid).StartWork);

                        pointstmt = con.prepareStatement(insertPoint);
                        pointstmt.setInt(1, 1);
                        pointstmt.setInt(2, EncPid);
                        pointstmt.setInt(3, order);
                        pointstmt.setInt(4, pointTime);//
                        pointstmt.executeUpdate();
                        
                        System.out.println(pointstmt);
                        System.out.println(EncPid);
                        System.out.println(order-1);
                        System.out.println(pointTime);
                        
                        JdbcUtils.close(pointstmt);

                        RoutingController.insertRouteLog(con, routeN, RouteLogType.POINT_ADDED, EncPid.toString(),
                                String.valueOf(order));

                        prevCust = pid;
                        order++;

                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Routes Save ERROR", e);
            return;
        } finally {
            JdbcUtils.close(pointstmt);
            JdbcUtils.close(routestmt);
        }

        RoutingUtils.updateRoutesCost(con, filter);
    }
	

	protected static void saveRoute(Connection con, int routeN, Matrix matrix, Riders result, AtmRouteFilter filter){ 
		RoutingUtils.clearRoute(con, routeN);
        PreparedStatement pointstmt = null;
        String insertPoint ="Insert into T_CM_ROUTE_POINT (ROUTE_ID, TYP, POINT_SRC_ID, ORD, POINT_TIME) Values(?, ?, ?, ?, ?) ";
		try {
			int order=1;
			int j=1;
			if (result!=null){
				
				for (int i=0; i<result.getRidersCount(); i++){
					if (result.getRiderATM(i).length>0) {
						order = 1;
						j=0;//j=1;
						for (Integer pid : result.getRiderATM(i)) {
							logger.debug("Saving point: ENC_ID:"+Integer.toString(pid)+" TIME:"+(result.getRiderTime(i, j)-matrix.getRiderTimeWindows().get(0).StartWork));
							pointstmt = con.prepareStatement(insertPoint);
							pointstmt.setInt(1, routeN);
							pointstmt.setInt(2, 1);
							pointstmt.setInt(3, pid);
							pointstmt.setInt(4, order++);
							pointstmt.setInt(5, result.getRiderTime(i, j++)-matrix.getRiderTimeWindows().get(0).StartWork);
							pointstmt.executeUpdate();
							JdbcUtils.close(pointstmt);
							
							RoutingController.insertRouteLog(con, routeN, RouteLogType.POINT_ADDED, pid.toString(), String.valueOf(order-1));
							
						}
					}
				}
			}
			
			
		} catch (SQLException e) {
			logger.error("Error while Saving Route",e);
		} finally {
			JdbcUtils.close(pointstmt);
		}
	}
	
	protected static void saveAtmsAsDefaultRoute(Connection con, AtmRouteFilter filter, Matrix matrix){
		RoutingUtils.clearRoutes(con, filter);
        PreparedStatement pointstmt = null;
        PreparedStatement routestmt = null;
        
        	
		try {
			String insertRoute ="Insert into T_CM_ROUTE (ID, ROUTE_DATE, ORG_ID, ROUTE_STATUS, RESULT_FLAG) Values("+JdbcUtils.getNextSequence(con, "SQ_CM_ROUTE")+", ?, ?, 1, ?) ";
			String insertPoint ="Insert into T_CM_ROUTE_POINT (ROUTE_ID, TYP, POINT_SRC_ID, ORD, VISITED_FLAG) Values("+JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE")+", ?, ?, ?, 0) ";
			
			pointstmt = con.prepareStatement(insertPoint);
			routestmt = con.prepareStatement(insertRoute);
			int order=2;
			routestmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
			routestmt.setInt(2, filter.getRegion());
			routestmt.setInt(3, matrix.constraintsStatus);//0
			routestmt.executeUpdate();

			order = 1;
			for (int i=1;i<matrix.ENC.length;i++) {
				pointstmt.setInt(1, 1);
				pointstmt.setInt(2, matrix.ENC[i]);
				pointstmt.setInt(3, order++);
				pointstmt.executeUpdate();

			}

		} catch (SQLException e) {
			logger.error("Default Route Save ERROR",e);
			return;
		} finally {
			JdbcUtils.close(pointstmt);
			JdbcUtils.close(routestmt);
		}
		
		RoutingUtils.updateDefaultRouteCostAndArrivalTimes(con, filter, matrix);
	}
	
	protected static int getRouteTimeFromPoints(Connection con, int routeN, int serviceTime){
		PreparedStatement prep = null;
		ResultSet rs = null;

		String query = "select COALESCE(max(POINT_TIME),0) as ROUTE_TIME "+
						"from T_CM_ROUTE_POINT  "+
						"where ROUTE_ID=? ";
		try {
			prep = con.prepareStatement(query);
			prep.setInt(1, routeN);
			rs = prep.executeQuery();
			rs.next();
			int routeTime = rs.getInt("ROUTE_TIME");
			if (routeTime>0){
				routeTime+=serviceTime;
			}
			return routeTime;
		} catch (SQLException e) {
			logger.error("Error getting Route Time",e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }
		return 0;
	}
	
	protected static void calculateArrivalTimes(Connection con,AtmRouteFilter filter, Matrix matrix, int routeN){
		String select = "Select pnt.TYP, pnt.ORD, pnt.ROUTE_ID, pnt.POINT_SRC_ID, nodes.TIME/60 as POINT_TIME "+ //*1000
				"from T_CM_ROUTE_NODES nodes, (select ORD, TYP, ROUTE_ID, POINT_SRC_ID, ATM_ID,LAG(T_CM_ENC_PLAN.ATM_ID, 1, NULL) OVER (ORDER BY ORD) AS prev_id from T_CM_ROUTE_POINT, T_CM_ENC_PLAN  where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID and  T_CM_ROUTE_POINT.route_id=?) pnt, T_CM_ROUTE route, T_CM_ROUTE_ORG org, T_CM_ROUTE_DEPOT depot "+
				"where pnt.ROUTE_ID=route.ID and route.ORG_ID=org.ID and org.DEPOT_ID=depot.ID and nodes.PIDB=pnt.ATM_ID "+
				 "and  nodes.PIDA=COALESCE(pnt.prev_id,depot.ID) and route.ID=? ORDER BY ORD";
		
		String update = "Update T_CM_ROUTE_POINT  set POINT_TIME=? where ROUTE_ID=? and POINT_SRC_ID=?";
		
		PreparedStatement timestmt = null;
		PreparedStatement updatestmt = null;
		ResultSet rs = null;
		long startTime=0;
		long currentTime = startTime;
		long transferTime = 0;
		long serviceTime = 0;
			
		try {
			timestmt = con.prepareStatement(select);
			timestmt.setInt(1, routeN);
			timestmt.setInt(2, routeN);
			rs = timestmt.executeQuery();
			
			updatestmt = con.prepareStatement(update);
			while(rs.next()){
				transferTime=rs.getInt("POINT_TIME");
				serviceTime=rs.getInt("ORD")==1 ? 0 : matrix.serviceTime[1];
				currentTime+=(transferTime+serviceTime);
				updatestmt.setLong(1, currentTime);
				updatestmt.setInt(2, routeN);
				updatestmt.setInt(3, rs.getInt("POINT_SRC_ID"));
				updatestmt.executeUpdate();
			}
		} catch (SQLException e) {
			logger.error("ERROR calculating point arrival duration for Route "+routeN,e);
		} finally {
			JdbcUtils.close(updatestmt);
	    	JdbcUtils.close(rs);
			JdbcUtils.close(timestmt);
	    }
	}
	
	protected static void clearRoutes(Connection con, AtmRouteFilter filter) {
		PreparedStatement prep = null;
		try {
				prep = con.prepareStatement("delete from T_CM_ROUTE_POINT where T_CM_ROUTE_POINT.ROUTE_ID in (select ID from T_CM_ROUTE where T_CM_ROUTE.ROUTE_DATE=? and T_CM_ROUTE.ORG_ID=? and T_CM_ROUTE.ROUTE_STATUS=1) ");
				prep.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
				prep.setInt(2, filter.getRegion());
				prep.executeUpdate();
				JdbcUtils.close(prep);
				
				prep = con.prepareStatement("delete from T_CM_ROUTE where T_CM_ROUTE.ROUTE_DATE=? and T_CM_ROUTE.ORG_ID=? and T_CM_ROUTE.ROUTE_STATUS=1 ");
				prep.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
				prep.setInt(2, filter.getRegion());
				prep.executeUpdate();
				JdbcUtils.close(prep);
		}catch (Exception e) {
			logger.error("Routes clearing ERROR",e);
		} finally {
			JdbcUtils.close(prep);
		}

	}
	
	protected static void clearRoute(Connection con, int routeN) {
		PreparedStatement prep = null;
		try {
				prep = con.prepareStatement("delete from T_CM_ROUTE_POINT where T_CM_ROUTE_POINT.ROUTE_ID = ? ");
				prep.setInt(1, routeN);
				prep.executeUpdate();
				prep.close();
		}catch (Exception e) {
			logger.error("Error while clearing route",e);
		} finally {
			JdbcUtils.close(prep);
		}

	}
	
	protected static void clearInProgressRoute(Connection con, int routeN) {
		PreparedStatement prep = null;
		try {
				prep = con.prepareStatement("delete from T_CM_ROUTE_POINT where T_CM_ROUTE_POINT.ROUTE_ID = ? and COALESCE(T_CM_ROUTE_POINT.VISITED_FLAG,0)<>1");
				prep.setInt(1, routeN);
				prep.executeUpdate();
				prep.close();
		}catch (Exception e) {
			logger.error("Error while clearing In Progress Route",e);
		} finally {
			JdbcUtils.close(prep);
		}

	}

	protected static int getDefaultRoutePointsCount(Connection con, AtmRouteFilter filter){
		PreparedStatement prep = null;
		ResultSet rs = null;

		String query = 
				"select count(1) as CNT " +
				"from t_cm_enc_plan ep "+
				    "join t_cm_atm a on (ep.atm_id = a.atm_id) "+
				"where not exists ( "+
				    "select null "+
				    "from t_cm_route r "+
				        "join t_cm_route_point rp on (rp.ROUTE_ID = r.ID) "+
				    "where rp.point_src_id = ep.enc_plan_id "+
				        "and trunc(r.route_date) = trunc(ep.date_forthcoming_encashment) "+
				") "+
				"and trunc(ep.date_forthcoming_encashment) = ? "+
				"and ep.ENC_REQ_ID IS NOT NULL "+
				"and ep.atm_id in (select atm_id from t_cm_route_atm2org where org_id = ?) ";
		
		try {
	        prep = con.prepareStatement(query);
	        prep.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
	        prep.setInt(2, filter.getRegion());
	        rs = prep.executeQuery();
	        if (rs.next()){
	        	return rs.getInt("CNT");
	        }
	    } catch (SQLException e) {
        	logger.error("Points get ERROR for default_route, date "+filter.getDateStart(),e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }

		return 0;
	}
	
	protected static String getRouteDepot(Connection con, int routeID){
		String queryDepotID = "select DEPOT_ID "+
				"from T_CM_ROUTE, T_CM_ROUTE_ORG "+
				"where T_CM_ROUTE.ORG_ID=T_CM_ROUTE_ORG.ID and T_CM_ROUTE.ID = ?";
		PreparedStatement prep = null;
		ResultSet rs = null;
		String depotID="0";
		try {
	        prep = con.prepareStatement(queryDepotID);
	        prep.setInt(1, routeID);
	        rs = prep.executeQuery();
	        if (rs.next()){
	        	depotID=rs.getString("DEPOT_ID");
	        }
	        
	        if (depotID=="0"){
	        	throw new SQLException("No Depot for Organization");
	        }
        } catch (SQLException e) {
	        logger.error("Depot get ERROR for Route "+routeID, e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }
		return depotID;
	}
	
	protected static String getOrgDepot(Connection con, int orgId){
		String queryDepotID = "select DEPOT_ID "+
				"from T_CM_ROUTE_ORG "+
				"where ID = ?";
		PreparedStatement prep = null;
		ResultSet rs = null;
		String depotID="0";
		try {
	        prep = con.prepareStatement(queryDepotID);
	        prep.setInt(1, orgId);
	        rs = prep.executeQuery();
	        if (rs.next()){
	        	depotID=rs.getString("DEPOT_ID");
	        }
	        
	        if (depotID=="0"){
	        	throw new SQLException("No Depot for Organization");
	        }
        } catch (SQLException e) {
	        logger.error("Depot get ERROR for Org "+orgId, e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }
		return depotID;
	}

}
