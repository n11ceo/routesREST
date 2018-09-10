package ru.bpc.cm.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bpc.cm.cashmanagement.CmCommonController;
import ru.bpc.cm.constants.CashManagementConstants;
import ru.bpc.cm.items.enums.RouteLogType;
import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.items.routes.*;
import ru.bpc.cm.items.routing.Matrix;
import ru.bpc.cm.items.routing.RiderTimeWindow;
import ru.bpc.cm.items.routing.SolutionRoutes;
import ru.bpc.cm.items.routing.TimeWindow;
import ru.bpc.cm.items.routing.VNS.InitialSol.Init;
import ru.bpc.cm.items.routing.VNS.InitialSol.MySol;
import ru.bpc.cm.items.routing.antmethod.ACOSolver.AntColony;
import ru.bpc.cm.items.routing.antmethod.Common.Customer;
import ru.bpc.cm.items.routing.antmethod.Common.Depot;
import ru.bpc.cm.items.routing.antmethod.Common.Place;
import ru.bpc.cm.items.routing.antmethod.Common.Vehicle;
import ru.bpc.cm.items.routing.antmethod.MDVRPTW.MDVRPTW;
import ru.bpc.cm.items.routing.antmethod.SDVRPTW.SDVRPTW;
import ru.bpc.cm.items.routing.heneticmethod.Henetic;
import ru.bpc.cm.items.routing.heneticmethod.Rider;
import ru.bpc.cm.items.routing.heneticmethod.Riders;
import ru.bpc.cm.items.routing.pareto.Pareto;
import ru.bpc.cm.utils.CmUtils;
import ru.bpc.cm.utils.db.JdbcUtils;
import ru.bpc.structs.collection.SeparatePrintedCollection;

import java.sql.*;
import java.util.*;
import java.util.Date;

//import ru.bpc.cm.items.routing.tabu.com.vrp.SVRPTW;


public class RoutingController {

	private static final Logger logger = LoggerFactory.getLogger("CASH_MANAGEMENT");

	public static ArrayList<AtmRoutePointItem> getPoints(Connection con, int routeN){
		PreparedStatement prep = null;
		ResultSet rs = null;
		ArrayList<AtmRoutePointItem> pointList = new ArrayList<AtmRoutePointItem>();
		String query =
				"select atmp.ROUTE_ID, atmp.TYP, enc.ATM_ID, atmp.POINT_SRC_ID as ENC_ID, atmp.ORD,atmp.VISITED_FLAG, atmp.REORDER_FLAG, " +
						"info.longitude, info.latitude, info.state, info.city, info.street," +
						"POINT_TIME, act.ATM_STATE "+
				"from T_CM_ROUTE_POINT atmp, T_CM_ATM info, T_CM_ATM_ACTUAL_STATE act, T_CM_ENC_PLAN enc "+
				"where atmp.POINT_SRC_ID=enc.ENC_PLAN_ID and enc.ATM_ID=info.ATM_ID and info.ATM_ID=act.ATM_ID and atmp.ROUTE_ID=? "+
				" order by ord";
		String depotID  = RoutingUtils.getRouteDepot(con, routeN);
		String[] depotInfo = getDepotInfo(con, depotID);
		int cnt = 0;
		try {
	        prep = con.prepareStatement(query);
	        prep.setInt(1, routeN);
	        rs = prep.executeQuery();
	        pointList.add(new AtmRoutePointItem(1, 0, depotID, depotInfo[0], depotInfo[1], depotInfo[2]+", "+depotInfo[3], 0, false, false, true, false));
	        while (rs.next()){
	        	cnt++;
	        	pointList.add(new AtmRoutePointItem(rs.getInt("ORD")+1,
	        			rs.getInt("ENC_ID"),
	        			String.valueOf(rs.getInt("ATM_ID")),
	        			rs.getString("latitude"),
	        			rs.getString("longitude"),
	        			rs.getString("city")+", "+rs.getString("street"),
	        			rs.getInt("POINT_TIME"), rs.getBoolean("VISITED_FLAG"), rs.getBoolean("REORDER_FLAG"), false, rs.getBoolean("ATM_STATE")));
	        }
	        pointList.add(new AtmRoutePointItem(cnt+2, 0, depotID, depotInfo[0], depotInfo[1], depotInfo[2]+", "+depotInfo[3], 0, false, false, true, false));
        } catch (SQLException e) {
        	logger.error("Points get ERROR for Route "+routeN,e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }

		return pointList;
	}

	public static ArrayList<AtmRoutePointItem> getPoints(Connection con, AtmRouteFilter filter){
		PreparedStatement prep = null;
		ResultSet rs = null;
		ArrayList<AtmRoutePointItem> pointList = new ArrayList<AtmRoutePointItem>();
		int ord = 1;

		String query =
				"select a.ATM_ID as ATM_ID, ep.ENC_PLAN_ID as ENC_ID, a.latitude,a.longitude,a.city,a.street, 0 as POINT_TIME, at.ATM_STATE " +
				"from t_cm_enc_plan ep "+
				    "join t_cm_atm a on (ep.atm_id = a.atm_id) "+
				    "left outer join t_cm_atm_actual_state at on (at.atm_id = ep.atm_id) "+
				"where not exists ( "+
				    "select null "+
				    "from t_cm_route r "+
				        "join t_cm_route_point rp on (rp.ROUTE_ID = r.ID) " +
				        "join t_cm_enc_plan ep1 on (ep1.ENC_PLAN_ID=rp.POINT_SRC_ID) "+
				    "where ep1.ATM_ID = ep.ATM_ID "+
				        "and trunc(r.route_date) = trunc(ep.date_forthcoming_encashment) "+
				") "+
				"and trunc(ep.date_forthcoming_encashment) = ? "+
				"and ep.ENC_REQ_ID IS NOT NULL "+
				"and ep.atm_id in (select atm_id from t_cm_route_atm2org where org_id = ?) "+
				"order by ATM_ID";
		String depotID  = RoutingUtils.getOrgDepot(con, filter.getRegion());
		String[] depotInfo = getDepotInfo(con, depotID);

		try {
	        prep = con.prepareStatement(query);
	        prep.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
	        prep.setInt(2, filter.getRegion());
	        rs = prep.executeQuery();
	        pointList.add(new AtmRoutePointItem(ord++, 0, depotID, depotInfo[0], depotInfo[1], depotInfo[2]+", "+depotInfo[3], 0, false, false, true, false));
	        while (rs.next()){
	        	pointList.add(new AtmRoutePointItem(ord++,
	        			rs.getInt("ENC_ID"),
	        			String.valueOf(rs.getInt("ATM_ID")),
	        			rs.getString("latitude"),
	        			rs.getString("longitude"),
	        			rs.getString("city")+", "+rs.getString("street"),
	        			rs.getInt("POINT_TIME"), false, false, false, rs.getBoolean("ATM_STATE")));
	        }
	        pointList.add(new AtmRoutePointItem(ord++, 0, depotID, depotInfo[0], depotInfo[1], depotInfo[2]+", "+depotInfo[3], 0, false, false, true, false));
	    } catch (SQLException e) {
        	logger.error("Points get ERROR for default_route, date "+filter.getDateStart(),e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }

		return pointList;
	}

	public static String[] getDepotInfo(Connection con, String depot) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String query = "select longitude, latitude, city, street, case when (COALESCE(latitude,'_') <> '_' "
					+ " or COALESCE(longitude,'_')<>'_') then 'true' else 'false' end as coordexist "
					+ "from T_CM_ROUTE_DEPOT "
					+ " where  ID=?";
			pstmt = con.prepareStatement(query);
			pstmt.setString(1, depot);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				String[] result = { rs.getString("latitude"),
						rs.getString("longitude"), rs.getString("city"),
						rs.getString("street"), rs.getString("coordexist") };;
				return result;
			} else {
				String[] result = { "", "", "", "", "false" };
				return result;
			}

		} catch (Exception e) {
			logger.error("Depot Info get ERROR for Depot "+depot, e);
		} finally {
			JdbcUtils.close(rs);
        	JdbcUtils.close(pstmt);
		}
		return null;
	}
	public static ArrayList<AtmRouteItem> getRoutes(Connection con, AtmRouteFilter filter){
		PreparedStatement prep = null;
		ResultSet rs = null;

		ArrayList<AtmRouteItem> routeList = new ArrayList<AtmRouteItem>();
		if(filter.getRegion() > 0){
			routeList.add(new AtmRouteItem(CashManagementConstants.DEFAULT_ROUTE_NUMBER, filter.getRegion(), filter.getDateStart(), false, RoutingUtils.getDefaultRoutePointsCount(con,filter), 0, 0, 0, "NOT_DEFINED", RouteStatus.CREATED.getId(), 0));
		}

		 String query = "select ID, ROUTE_DATE, ORG_ID , RESULT_FLAG, COST, COST_CURR, ROUTE_TIME, ROUTE_LENGTH, atmp.POINTS, " +
						"ROUTE_STATUS "+
						"from T_CM_ROUTE left outer join (select count(ORD) as POINTS, ROUTE_ID from T_CM_ROUTE_POINT group by ROUTE_ID) atmp on atmp.ROUTE_ID=T_CM_ROUTE.ID "+
						"where ORG_ID=? and ROUTE_DATE=? " +
						"ORDER BY ID";
		try {
	        prep = con.prepareStatement(query);
	        prep.setInt(1, filter.getRegion());
	        prep.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
	        rs = prep.executeQuery();
	        while (rs.next()){
		        	routeList.add(
		        			new AtmRouteItem(rs.getInt("ID"), filter.getRegion(), filter.getDateStart(),
		        					false, rs.getInt("POINTS"), rs.getInt("ROUTE_LENGTH"),
		        					rs.getInt("ROUTE_TIME"),rs.getLong("COST"),CmCommonController.getCurrCodeA3(con, rs.getInt("COST_CURR")),rs.getInt("ROUTE_STATUS"),rs.getInt("RESULT_FLAG")));

	        }
        } catch (SQLException e) {
        	logger.error("Routes get ERROR",e);
        } finally {
        	JdbcUtils.close(rs);
			JdbcUtils.close(prep);
        }

		return routeList;
	}

	public void deleteWaypoints(Connection con, String uid, ArrayList<AtmRoutePointItem> pnts, int routeN){

		PreparedStatement prep = null;

		String query =
		        	"delete from T_CM_ROUTE_POINT atmp "+
					"where atmp.atm_route_n = ? and atmp.ord = ?";
		try {
			for (int i=0; i<pnts.size(); i++){


		        prep = con.prepareStatement(query);
		        prep.setInt(2, pnts.get(i).getN());
		        prep.setInt(1, routeN);
		        prep = con.prepareStatement(query);
		        prep.executeQuery();
			}
        } catch (SQLException e) {
        	logger.error("Waypoints removal ERROR for route "+routeN,e);
        } finally {
			JdbcUtils.close(prep);
        }

	}

	public static void addWaypoint(Connection con, String uid, AtmRoutePointItem pnt, int routeN){

		PreparedStatement prep = null;
		PreparedStatement prepinsert = null;
		PreparedStatement prepupdate = null;
		String query;

		try {

		        query =
		        	"update T_CM_ROUTE_POINT "+
					"set ord = ord +1"+
					"where ord >= ? and atm_route_n=?";
		        prepupdate = con.prepareStatement(query);
		        prepupdate.setInt(1, pnt.getN());
		        prepupdate.setInt(2, routeN);
		        prepupdate.executeQuery();
		        prepupdate.close();

		        query =
		        	"INSERT INTO T_CM_ROUTE_POINT "+
					"(n,atm_route_n,typ,pid,ord) " +
					"VALUES " +
					"("+JdbcUtils.getNextSequence(con, "SQ_CM_ROUTE_TMP")+",?,3,0,?)";
		        prepinsert = con.prepareStatement(query);
		        prepinsert.setInt(1, routeN);
		        prepinsert.setInt(2, pnt.getN());
		        prepinsert.executeQuery();
		        prepinsert.close();

		        query =
		        	"INSERT INTO T_CM_ROUTE_WAYPOINT "+
					"(n,latitude,longitude) " +
					"VALUES " +
					"("+JdbcUtils.getCurrentSequence(con, "SQ_CM_ROUTE_TMP")+",?,?)";
		        prep = con.prepareStatement(query);
		        prep.setString(1, pnt.getLatitude());
		        prep.setString(2, pnt.getLongitude());
		        prep.executeQuery();
		        prep.close();

        } catch (SQLException e) {
        	logger.error("Waypoint add ERROR for route "+routeN,e);
        } finally {
			JdbcUtils.close(prep);
        }

	}

	private static Matrix constructMatrix(Connection con, AtmRouteFilter filter) throws RoutingException{
        Matrix matrix = RoutingParamsUtils.getMatrixAndAtmsForCalculation(con ,filter);
        matrix.type = filter.getRoutingType();
        if (matrix.type==0) {
            matrix.type=1;
        }
        logger.debug("calculating with type:"+matrix.type);

        RoutingParamsUtils.fillAtmTimeWindows(con, filter, matrix);

        RoutingParamsUtils.fillRiderTimeWindows(con,  filter, matrix);

        RoutingParamsUtils.checkTimeWindows(matrix);

        RoutingParamsUtils.checkBasicConstraints(matrix);

        return matrix;
    }

    private static void logMatrix(Matrix matrix){
    	logger.debug("~~~~~~~~~~~~~~~~Matrix~~~~~~~~~~~~~~~~");
//		logger.debug("NORMAL_ATM_WINDOW_MODE = " + matrix.NORMAL_ATM_WINDOW_MODE);
//		logger.debug("DEFAULT_ATM_WINDOW_MODE = " + matrix.DEFAULT_ATM_WINDOW_MODE);
//		logger.debug("CONSTRAINTS_OK = " + matrix.CONSTRAINTS_OK);
//		logger.debug("TIME_CONSTRAINT_VIOLATION = " + matrix.TIME_CONSTRAINT_VIOLATION);
//		logger.debug("CARS_OR_ATMS_CONSTRAINT_VIOLATION = " + matrix.CARS_OR_ATMS_CONSTRAINT_VIOLATION);
//		logger.debug("SUMM_CONSTRAINT_VIOLATION = " + matrix.SUMM_CONSTRAINT_VIOLATION);

		StringBuilder demo = new StringBuilder();
		logger.debug("m = new Matrix(" + matrix.ENC.length + ");");
		for (int x : matrix.ENC) {
			demo.append(x + ", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.ENC = new int[]{" + demo + "};");

		demo = new StringBuilder();
		for (String x : matrix.ATM) {
			demo.append("\"" + x + "\", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.ATM = new String[]{" + demo + "};");

		//m.distanceCoeffs = 	new int[][]{{0, 47, 10, 30, 49}, {56, 0, 51, 76, 10}, {53, 6, 0, 24, 50}, {4, 17, 31, 0, 75},{22, 20, 5000, 5000, 0}};
		demo = new StringBuilder();
		for (int[] ar : matrix.distanceCoeffs) {
			demo.append("{");
			for (int a : ar){
				demo.append(a + ", ");
			}
			demo.setLength(demo.length() - 2);
			demo.append("}, ");

		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.distanceCoeffs = new int[][]{" + demo + "};");

		demo = new StringBuilder();
		for (int[] ar : matrix.timeCoeffs) {
			demo.append("{");
			for (int a : ar){
				demo.append(a + ", ");
			}
			demo.setLength(demo.length() - 2);
			demo.append("}, ");

		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.timeCoeffs = new int[][]{" + demo + "};");

		demo = new StringBuilder();
		for (TimeWindow x : matrix.getTimeWindows()) {
			logger.debug("m.addTimeWindow("+x.NumATM+", "+x.StartWork+", "+x.EndWork+", "+x.emergencyWindow+");");
		}

		demo = new StringBuilder();
		for (RiderTimeWindow x : matrix.getRiderTimeWindows()) {
			logger.debug("m.addRiderTimeWindow("+x.StartWork+", "+x.EndWork+");");
		}

		demo = new StringBuilder();
		for (int x : matrix.amountOfMoney) {
			demo.append(x + ", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.amountOfMoney = new int[]{" + demo + "};");

		demo = new StringBuilder();
		for (int x : matrix.serviceTime) {
			demo.append(x + ", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.serviceTime = new int[]{" + demo + "};");

		logger.debug("m.MaxMoney = " + matrix.MaxMoney+";");

		demo = new StringBuilder();
		for (int x : matrix.amountOfCassettes) {
			demo.append(x + ", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.amountOfCassettes = new int[]{" + demo + "};");

		logger.debug("m.VolumeOneCar = " + matrix.VolumeOneCar+";");
		logger.debug("m.FixPrice = " + matrix.FixPrice+";");
		logger.debug("m.LengthPrice = " + matrix.LengthPrice+";");
		logger.debug("m.MaxATMInWay = " + matrix.MaxATMInWay+";");
		logger.debug("m.MaxTime = " + matrix.MaxTime+";");
		logger.debug("m.MaxLength = " + matrix.MaxLength+";");
		logger.debug("m.depot = \"" + matrix.depot+"\";");
		logger.debug("m.maxCars = " + matrix.maxCars+";");

		demo = new StringBuilder();
		for (double x : matrix.AtmPrice) {
			demo.append(x + ", ");
		}
		demo.setLength(demo.length() - 2);
		logger.debug("m.AtmPrice = new double[]{" + demo + "};");

		logger.debug("m.currCode = " + matrix.currCode+";");
		logger.debug("m.windowMode = " + matrix.windowMode+";");
		logger.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }

    private static void logMatrix(Matrix m, Riders riders){
    	logMatrix(m);
    	logger.debug("~~~~~~~~~~~~~~~Riders~~~~~~~~~~~~~~~~~~~");
        for (Rider r : riders.riderList) {
            logger.debug("  Rider:");
            logger.debug("    atmList = " + Arrays.toString(r.GetAtmList().toArray()));
            logger.debug("    cost = " + Arrays.toString(r.cost.toArray()));
            logger.debug("    time = " + Arrays.toString(r.time.toArray()));
            logger.debug("    timewait = " + Arrays.toString(r.timewait.toArray()));
            logger.debug("    length = " + Arrays.toString(r.length.toArray()));
            logger.debug("    sumWaitTime = " + r.sumWaitTime);
            logger.debug("    sumTime = " + r.sumTime);
            logger.debug("    Money = " + r.Money);
            logger.debug("    Cassettes = " + r.Cassettes);
            logger.debug("    SumLength = " + r.SumLength);
            logger.debug("    WaitInDepoTime = " + r.WaitInDepoTime);
            logger.debug("    WayNum = " + r.WayNum);
        }
        logger.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }


	public static void calculateRoutesGenetic(Connection con, String uid,AtmRouteFilter filter) throws RoutingException {
		Matrix matrix = constructMatrix(con, filter);
		logger.debug("~~~~~~~~~~~~~~~Genetic~~~~~~~~~~~~~~~~~~~");
        Henetic henetic = new Henetic();

        double lastFunctionValue=0;

        if (matrix.ENC.length-1>=3){
        	int ridersCount=matrix.maxCars;
        	int currentRidersCount = 2;
        	Riders result = henetic.getBestResult(matrix,currentRidersCount);
        	while (!result.isGood()){
        		if (currentRidersCount<=ridersCount){
        			currentRidersCount++;
        		}
        		if (currentRidersCount>ridersCount) {
        			if (matrix.windowMode==Matrix.NORMAL_ATM_WINDOW_MODE){
        				matrix.windowMode=Matrix.DEFAULT_ATM_WINDOW_MODE;
        				RoutingParamsUtils.fillAtmTimeWindows(con, filter, matrix);
        				currentRidersCount = 2;
        			} else{
        				break;
        			}
        		}
        		result = henetic.getBestResult(matrix, currentRidersCount);
        	}
        	logMatrix(matrix, result);
            
        	/*if (result.isGood()){
        		while ((lastFunctionValue==0 || lastFunctionValue>result.getActualFunctionValue()) && currentRidersCount<ridersCount){
        			currentRidersCount++;
	        		result = henetic.getBestResult(matrix,currentRidersCount);
        		}
        	}*/



        	if (!result.isGood()) {

        		logger.debug("Bad solution!!!");
        		logger.debug("windows count: "+matrix.getTimeWindows().size());
        	}
        	for (int i = 0; i < result.getRidersCount(); i++) {
        		for (int j = 0; j < matrix.getWayNumber(); j++) {//TODO: remove ways handling, not needed
        			logger.debug("Rider Number: " + (i + 1) + "Route Number"
        					+ (j + 1) + " Route time: " + result.getRiderTime(i, j)
        					+ " Route cost: " + result.getRiderCost(i, j)
        					+ " Route length: " + result.getRiderLength(i, j)
        					+ " Rider money: " + result.getRiderMoney(i)
        					+ " Route: "
        					+ Arrays.toString(result.getRiderATM(i)));
        		}
        	}
        	RoutingUtils.saveRoutes(con, filter, matrix, result);
        } else{
        	logger.debug("Saving to Single Route");
        	RoutingUtils.saveAtmsAsDefaultRoute(con, filter, matrix);
        }
	}

	public static void recalculateRoute(Connection con, AtmRouteFilter filter,
			int routeN) throws RoutingException {
		PreparedStatement prep = null;
		ResultSet rs = null;

		String allowedQuery = "select min(ord) as allowedOrd from T_CM_ROUTE_POINT where route_id=? and COALESCE(VISITED_FLAG,0)=0";
		String cntQuery = "select count(*) as cnt from T_CM_ROUTE_POINT where T_CM_ROUTE_POINT.route_id=? and T_CM_ROUTE_POINT.typ<>2 ";

        try {
        	//checking min ord, allowed to modify
        	int allowedOrd = 1;

        	if (routeN!=CashManagementConstants.DEFAULT_ROUTE_NUMBER){
        		prep = con.prepareStatement(allowedQuery);
	    		prep.setInt(1, routeN);
	        	rs = prep.executeQuery();
	        	rs.next();
	        	allowedOrd = rs.getInt("allowedOrd");
	        	JdbcUtils.close(rs);
	        	JdbcUtils.close(prep);

	        	prep = con.prepareStatement(cntQuery);
		        prep.setInt(1, routeN);
	        	rs = prep.executeQuery();
	        	rs.next();

				optimizeRouteGenetic(con, (rs.getInt("cnt")-allowedOrd+1<=1 ? 0 : rs.getInt("cnt")-allowedOrd+1), allowedOrd>1, filter, routeN, true);
				JdbcUtils.close(rs);
	        	JdbcUtils.close(prep);
        	}

        } catch (SQLException e) {
	        logger.error("Error updating Route",e);
        } finally {
        	JdbcUtils.close(rs);
        	JdbcUtils.close(prep);
        }

        RoutingUtils.updateRoutesCost(con, filter);
	}

	public static void optimizeRouteGenetic(Connection con, int cnt, boolean inProgress, AtmRouteFilter filter, int routeN, boolean singleRouteRefresh) throws RoutingException{
		if (!inProgress){
			if (cnt>0){
				Matrix matrix = RoutingParamsUtils.getMatrixAndAtmsFromRoute(con , filter, routeN);
				matrix.type = filter.getRoutingType();
				if (matrix.type==0) {
					matrix.type=1;
				}
				logger.debug("calculating with type:"+matrix.type);
				RoutingParamsUtils.fillAtmTimeWindows(con, filter, matrix);

				RoutingParamsUtils.fillRiderTimeWindows(con,  filter, matrix);

				RoutingParamsUtils.setOrgParametersForRouting(con, matrix, filter.getRegion());

		        RoutingParamsUtils.checkTimeWindows(matrix);

		        RoutingParamsUtils.checkBasicConstraintsForSingleRoute(matrix);

		        int ridersCount=1;

				if (cnt>2 && matrix.ENC.length-1>2){
					logger.debug("Optimizing route "+routeN);

					logger.debug(Arrays.toString(matrix.ENC));
			        Henetic henetic = new Henetic();

			        Riders result = henetic.getBestResult(matrix,ridersCount);
			        if (!result.isGood()) {
			        	if (matrix.windowMode==Matrix.NORMAL_ATM_WINDOW_MODE){
		        			matrix.windowMode=Matrix.DEFAULT_ATM_WINDOW_MODE;
		        			RoutingParamsUtils.fillAtmTimeWindows(con, filter, matrix);
		        			henetic = new Henetic();

					        result = henetic.getBestResult(matrix,ridersCount);
					        if (result.isGood()){
					        	for(int i=0; i<result.getRidersCount(); i++){
									logger.debug("Rider Number: "+(i+1)+" Route time: "+result.getRiderTime(i,0)+" Route length: "+result.getRiderLength(i,0)+" Route money: "+result.getRiderMoney(i)+" Route: "+Arrays.toString(result.getRiderATM(i)));
								}
					        	RoutingUtils.saveRoute(con, routeN, matrix, result, filter);
								RoutingUtils.updateRouteResult(con, routeN, true, inProgress, singleRouteRefresh, matrix);
					        } else{
					        	logger.debug("Bad solution!!!");
			        			RoutingUtils.updateRouteResult(con, routeN, false, inProgress, singleRouteRefresh, matrix);
			        			RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
					        }
		        		} else{
		        			logger.debug("Bad solution!!!");
		        			RoutingUtils.updateRouteResult(con, routeN, false, inProgress, singleRouteRefresh, matrix);
		        			RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
		        		}

					} else {
						for(int i=0; i<result.getRidersCount(); i++){
							logger.debug("Rider Number: "+(i+1)+" Route time: "+result.getRiderTime(i,0)+" Route length: "+result.getRiderLength(i,0)+" Route money: "+result.getRiderMoney(i)+" Route: "+Arrays.toString(result.getRiderATM(i)));
						}
						RoutingUtils.saveRoute(con, routeN, matrix, result, filter);
						RoutingUtils.updateRouteResult(con, routeN, true, inProgress, singleRouteRefresh, matrix);
					}

				} else {
					RoutingUtils.updateRouteResult(con, routeN, RoutingParamsUtils.checkBasicConstraintsForSingleRoute(matrix), inProgress, singleRouteRefresh, matrix);
					RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
				}


		        //calculateArrivalTimes(con, filter, matrix, routeN);
			}
		} else {
			if (cnt>0){
				Matrix matrix = RoutingParamsUtils.getMatrixAndAtmsFromInProgressRoute(con , filter, routeN);
				RoutingParamsUtils.fillAtmTimeWindows(con, filter, matrix);

				RoutingParamsUtils.fillRiderTimeWindows(con,  filter, matrix);

				RoutingParamsUtils.setOrgParametersForRouting(con, matrix, filter.getRegion());

				RoutingParamsUtils.checkBasicConstraintsForSingleRoute(matrix);

		        int ridersCount=1;

				if (cnt>2 && matrix.ENC.length-1>2){
					logger.debug("Optimizing route(In Progress) "+routeN);

					logger.debug(Arrays.toString(matrix.ENC));
			        Henetic henetic = new Henetic();

			        Riders result = henetic.getBestResult(matrix,ridersCount);
			        if (!result.isGood()) {
			        	logger.debug("Bad solution!!!");
			        	RoutingUtils.updateInProgressRoute(con, routeN, matrix, result, filter);
			        	RoutingUtils.updateRouteResult(con, routeN, false, inProgress, singleRouteRefresh, matrix);
						RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
			        } else {
						for(int i=0; i<result.getRidersCount(); i++){
							logger.debug("Rider Number: "+(i+1)+" Route: "+Arrays.toString(result.getRiderATM(i)));
						}
						RoutingUtils.updateInProgressRoute(con, routeN, matrix, result, filter);
						if (result.riderList.get(0).time.size()==0){
							RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
						}

						RoutingUtils.updateRouteResult(con, routeN, true, inProgress, singleRouteRefresh, matrix);
					}

				} else {
					RoutingUtils.updateRouteResult(con, routeN, RoutingParamsUtils.checkBasicConstraintsForSingleRoute(matrix), inProgress, singleRouteRefresh, matrix);
					RoutingUtils.calculateArrivalTimes(con, filter, matrix, routeN);
				}


		        //calculateArrivalTimes(con, filter, matrix, routeN);
			}
		}

	}

	public static void saveRouteMatrix(Connection con,ArrayList<MatrixCoordItem> matrixPidList, String region) {

		PreparedStatement update = null;
		PreparedStatement prep = null;
		ResultSet rs = null;

		String query =
				"select COALESCE(distance,-1) as CNT " +
				"from T_CM_ROUTE_NODES " +
				"where " +
				"pida = ? " +
				"and " +
				"pidb = ? ";
		String queryUpdate =
				"UPDATE T_CM_ROUTE_NODES " +
				"SET " +
				"TIME = ?, " +
				"DISTANCE = ? "+
				"where " +
				"pida = ? " +
				"and " +
				"pidb = ? ";
		String queryInsert =
				"INSERT INTO T_CM_ROUTE_NODES "+
				"(PIDA,PIDB,DISTANCE,TIME) "+
				"VALUES "+
				"(?,?,?,?)";


		try {
			for(MatrixCoordItem item :matrixPidList){
				prep = con.prepareStatement(query);
				prep.setInt(1, Integer.parseInt(item.getPidA()));
				prep.setInt(2, Integer.parseInt(item.getPidB()));
				rs = prep.executeQuery();

				if(rs.next()){
					update = con.prepareStatement(queryUpdate);
					update.setInt(1, Integer.parseInt(item.getTime()));
					update.setInt(2, Integer.parseInt(item.getDistance()));
					update.setInt(3, Integer.parseInt(item.getPidA()));
					update.setInt(4, Integer.parseInt(item.getPidB()));
					update.executeUpdate();
					JdbcUtils.close(update);
				} else {
					update = con.prepareStatement(queryInsert);
					update.setInt(1, Integer.parseInt(item.getPidA()));
					update.setInt(2, Integer.parseInt(item.getPidB()));
					update.setInt(3, Integer.parseInt(item.getDistance()));
					update.setInt(4, Integer.parseInt(item.getTime()));
					update.executeUpdate();
					JdbcUtils.close(update);
				}
				JdbcUtils.close(rs);
				JdbcUtils.close(prep);
			}
		}catch (Exception e) {
			logger.error("Error while saving nodes",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(prep);
			JdbcUtils.close(update);
		}

	}


	public static void insertRouteLog(Connection connection, int routeId, RouteLogType logType, String... params) {

		PreparedStatement pstmt = null;

		try {
			String query =
			        "INSERT INTO T_CM_ROUTE_LOG " +
	                "(ROUTE_ID,LOG_DATE,LOG_TYPE,LOG_ID,PARAMS) " +
	                "VALUES " +
	                "(?,?,?,"+JdbcUtils.getNextSequence(connection, "s_enc_plan_log_id")+",?)";
			pstmt = connection.prepareStatement(query);
			pstmt.setInt(1, routeId);
			pstmt.setTimestamp(2,new Timestamp(new Date().getTime()));
			pstmt.setInt(3, logType.getId());
			pstmt.setString(4, new SeparatePrintedCollection<String>(Arrays.asList(params)).toString());

			pstmt.executeUpdate();

		} catch (Exception e) {
			logger.error("", e);
		} finally {
			JdbcUtils.close(pstmt);
		}
	}


}
