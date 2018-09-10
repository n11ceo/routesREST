package ru.bpc.cm.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bpc.cm.cashmanagement.CmCommonController;
import ru.bpc.cm.forecasting.anyatm.CheckerController;
import ru.bpc.cm.items.encashments.AtmEncashmentItem;
import ru.bpc.cm.items.enums.AtmAttribute;
import ru.bpc.cm.items.enums.EncashmentType;
import ru.bpc.cm.items.enums.OrgAttribute;
import ru.bpc.cm.items.forecast.ForecastException;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.RoutingException;
import ru.bpc.cm.items.routing.Matrix;
import ru.bpc.cm.items.routing.RiderTimeWindow;
import ru.bpc.cm.items.routing.TimeWindow;
import ru.bpc.cm.items.settings.AtmGroupAttributeItem;
import ru.bpc.cm.settings.AtmInfoController;
import ru.bpc.cm.utils.CmUtils;
import ru.bpc.cm.utils.db.JdbcUtils;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class RoutingParamsUtils {
	private static final Logger logger = LoggerFactory.getLogger("CASH_MANAGEMENT");
	
protected static Matrix getMatrixAndAtmsForCalculation(Connection con ,AtmRouteFilter filter) throws RoutingException{//parameters: date and region

		
		ArrayList<AtmEncashmentItem> atms = getAtmsForCalculation(con, filter);
		
		if (atms.size()-1==0 || atms.size()==0){//atms.size()<2
			RoutingUtils.clearRoutes(con, filter);
			throw new RoutingException(3);
		}
			
		
		Matrix matrix = new Matrix(atms.size());
		
		setOrgParametersForRouting(con, matrix, filter.getRegion());
		
		matrix = fillMatrix(con ,filter, matrix, atms);
		
        
		return matrix;
            
	}
	
	private static ArrayList<AtmEncashmentItem> getAtmsForCalculation(
			Connection con, AtmRouteFilter filter) {
		PreparedStatement pstmt = null;
		ResultSet result = null;
		ArrayList<AtmEncashmentItem> atms = new ArrayList<AtmEncashmentItem>();
		String sql = 
				"select DEPOT_ID as POINT_ID, 0 as ENC_ID, 0 as POINT_ORD, 0 as ENCASHMENT_TYPE from T_CM_ROUTE_ORG where ID = ? " +
				"union " +
				"select distinct T_CM_ENC_PLAN.ATM_ID as POINT_ID, T_CM_ENC_PLAN.ENC_PLAN_ID as ENC_ID, 1 as POINT_ORD, T_CM_ENC_PLAN.ENCASHMENT_TYPE as ENCASHMENT_TYPE " +
						"from T_CM_ENC_PLAN,T_CM_ROUTE_ATM2ORG " +
						"where T_CM_ENC_PLAN.ATM_ID=T_CM_ROUTE_ATM2ORG.ATM_ID and trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT)=? "+
						"and T_CM_ENC_PLAN.ENCASHMENT_TYPE != 0 "+
						"and T_CM_ENC_PLAN.ENC_REQ_ID IS NOT NULL "+
						"and T_CM_ROUTE_ATM2ORG.ORG_ID=? " +
						"and T_CM_ENC_PLAN.ENC_PLAN_ID not in (select DISTINCT T_CM_ROUTE_POINT.POINT_SRC_ID from T_CM_ROUTE, T_CM_ROUTE_POINT where T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID and T_CM_ROUTE.ROUTE_STATUS>1 AND T_CM_ROUTE.ROUTE_DATE=?) " +//check for approved and in-progress routes
				"ORDER BY POINT_ORD,POINT_ID ";
		try {
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, filter.getRegion());
			pstmt.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setInt(3, filter.getRegion());
			pstmt.setDate(4, JdbcUtils.getSqlDate(filter.getDateStart()));
			result = pstmt.executeQuery();
			logger.debug("ATMs for Calculation:");
			while (result.next()){
				AtmEncashmentItem item = new AtmEncashmentItem();
				item.setAtmID(Integer.valueOf(result.getString("POINT_ID")));
				item.setEncPlanID(result.getInt("ENC_ID"));
				if (result.getInt("ENCASHMENT_TYPE")==3){
					item.setEncType(EncashmentType.CASH_OUT);
				} else if (result.getInt("ENCASHMENT_TYPE")==2){
					item.setEncType(EncashmentType.CASH_IN);
				} else if (result.getInt("ENCASHMENT_TYPE")==1){
					item.setEncType(EncashmentType.CASH_IN_AND_CASH_OUT);
				} else {
					item.setEncType(EncashmentType.NOT_NEEDED);
				}
				atms.add(item);
				logger.debug("ATM: "+item.getAtmID()+" ENC_PLAN_ID: "+item.getEncPlanID()+" ENC_TYPE: "+item.getEncType().getId());
			}
		} catch (SQLException e) {
			logger.error("ERROR while getting ATMs for calculation. Region ID: "+filter.getRegion()+" Date: "+filter.getDateStart(),e);
			
		} finally {
        	JdbcUtils.close(result);
			JdbcUtils.close(pstmt);
        }
		
		getEncashmentSumsForAtms(con,filter, atms);
		
		return atms;
	}

	protected static Matrix getMatrixAndAtmsFromRoute(Connection con ,AtmRouteFilter filter, int routeN) throws RoutingException{//parameters: date and region
		
		
		ArrayList<AtmEncashmentItem> atms = getAtmsFromRoute(con, filter, routeN);
		
		Matrix matrix = new Matrix(atms.size());
		
		setOrgParametersForRouting(con, matrix, filter.getRegion());
		
	    matrix = fillMatrixFromRoute(con , filter, matrix, atms, routeN);
		
		return matrix;
	        
	}
	
protected static Matrix getMatrixAndAtmsFromInProgressRoute(Connection con ,AtmRouteFilter filter, int routeN) throws RoutingException{//parameters: date and region
		
		
		ArrayList<AtmEncashmentItem> atms = getAtmsFromInProgressRoute(con, filter, routeN);
		
		Matrix matrix = new Matrix(atms.size());
		
		setOrgParametersForRouting(con, matrix, filter.getRegion());
		
	    matrix = fillMatrixFromInProgressRoute(con , filter, matrix, atms, routeN);
		
		return matrix;
	        
	}

	private static ArrayList<AtmEncashmentItem> getAtmsFromRoute(Connection con,
			AtmRouteFilter filter, int routeN) {
		PreparedStatement pstmt = null;
		ResultSet result = null;
		ArrayList<AtmEncashmentItem> atms = new ArrayList<AtmEncashmentItem>();
		String sql = "select '0' as POINT_SRC_ID, DEPOT_ID as ATM_ID, 0 as POINT_ORD from T_CM_ROUTE_ORG where ID = ?   "+
					 "union " +
					 "select distinct to_char(POINT_SRC_ID) as POINT_SRC_ID, T_CM_ENC_PLAN.ATM_ID as ATM_ID, 1 as POINT_ORD from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN "+ //+trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT) = T_CM_ROUTE.ROUTE_DATE
					 "where route_id=? AND T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID AND T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ROUTE_POINT.POINT_SRC_ID "+//T_CM_ENC_PLAN.ATM_ID=T_CM_ROUTE_POINT.POINT_SRC_ID
	                 "ORDER BY POINT_ORD,POINT_SRC_ID ";
		
		try {
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, filter.getRegion());
			pstmt.setInt(2, routeN);
			result = pstmt.executeQuery();
			logger.debug("ATMs for Calculation from Route "+routeN+" :");
			while (result.next()){
				AtmEncashmentItem item = new AtmEncashmentItem();
				item.setAtmID(Integer.valueOf(result.getString("ATM_ID")));
				item.setEncPlanID(result.getInt("POINT_SRC_ID"));
				atms.add(item);
				logger.debug("ATM: "+item.getAtmID()+" ENC_PLAN_ID: "+item.getEncPlanID());
			}
		} catch (SQLException e) {
			logger.error("ERROR while getting ATMs for calculation. Region ID: "+filter.getRegion()+" Date: "+filter.getDateStart(),e);
		} finally {
	    	JdbcUtils.close(result);
			JdbcUtils.close(pstmt);
	    }
		getEncashmentSumsForAtms(con,filter, atms);
		
		return atms;
	}
	
	private static ArrayList<AtmEncashmentItem> getAtmsFromInProgressRoute(Connection con,
			AtmRouteFilter filter, int routeN) {
		PreparedStatement pstmt = null;
		ResultSet result = null;
		ArrayList<AtmEncashmentItem> atms = new ArrayList<AtmEncashmentItem>();
		String sql = "select points.POINT_SRC_ID, points.ATM_ID, points.POINT_ORD from (select POINT_SRC_ID, T_CM_ENC_PLAN.ATM_ID as ATM_ID, 0 as POINT_ORD, T_CM_ROUTE_POINT.VISITED_FLAG, LEAD(T_CM_ROUTE_POINT.VISITED_FLAG,1,1)  OVER (ORDER BY ORD) AS LEAD_VISITED_FLAG from T_CM_ROUTE_POINT, T_CM_ROUTE, T_CM_ENC_PLAN where T_CM_ROUTE.ID=? AND T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID AND T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) points where COALESCE(points.VISITED_FLAG,0)=1 AND points.LEAD_VISITED_FLAG<>1 "+
					 "union "+
					 "select distinct POINT_SRC_ID, T_CM_ENC_PLAN.ATM_ID as ATM_ID, 1 as POINT_ORD from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN " +
					 "where route_id=? AND COALESCE(T_CM_ROUTE_POINT.VISITED_FLAG,0)<>1 AND T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID AND T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID "+// AND T_CM_ENC_PLAN.ATM_ID=T_CM_ROUTE_POINT.POINT_SRC_ID and trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT) = T_CM_ROUTE.ROUTE_DATE
	                 "ORDER BY POINT_ORD,POINT_SRC_ID ";
		
		try {
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, routeN);
			pstmt.setInt(2, routeN);
			result = pstmt.executeQuery();
			logger.debug("ATMs for Calculation from Route(In Progress) "+routeN+" :");
			while (result.next()){
				AtmEncashmentItem item = new AtmEncashmentItem();
				item.setAtmID(Integer.valueOf(result.getString("ATM_ID")));
				item.setEncPlanID(result.getInt("POINT_SRC_ID"));
				atms.add(item);
				logger.debug("ATM: "+item.getAtmID()+" ENC_PLAN_ID: "+item.getEncPlanID());
			}
		} catch (SQLException e) {
			logger.error("ERROR while getting ATMs for calculation for route in progress "+routeN+". Region ID: "+filter.getRegion()+" Date: "+filter.getDateStart(),e);
		} finally {
	    	JdbcUtils.close(result);
			JdbcUtils.close(pstmt);
	    }
		
		getEncashmentSumsForAtms(con,filter, atms);
		
		return atms;
	}
	
	private static void getEncashmentSumsForAtms(Connection con,
			AtmRouteFilter filter, ArrayList<AtmEncashmentItem> atms) {
		PreparedStatement pstmt = null;
		ResultSet result = null;
		String sql = "select  T_CM_ENC_PLAN_CURR.CURR_SUMM, T_CM_ENC_PLAN_CURR.CURR_CODE from T_CM_ENC_PLAN, T_CM_ENC_PLAN_CURR " +
					 "where  T_CM_ENC_PLAN.ENC_PLAN_ID = ? and T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ENC_PLAN_CURR.ENC_PLAN_ID "+ //and trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT) = ?
	                 "ORDER BY  CURR_SUMM";
		
		try {
			double currentEncSumm=0;
			logger.debug("Encashment Sums:");
			for (AtmEncashmentItem item : atms){
				pstmt=con.prepareStatement(sql);
				pstmt.setInt(1, item.getEncPlanID());
				//pstmt.setString(1, item.getAtmID());
				//pstmt.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
				result = pstmt.executeQuery();
				currentEncSumm=0;
				while (result.next()){//for now, converting everything to RUR
					currentEncSumm+=CmCommonController.convertValue(con, result.getInt("CURR_CODE"), 810, Double.valueOf(result.getString("CURR_SUMM")), "9999");
					logger.debug("ATM: "+item.getAtmID()+" CURR_SUMM: "+currentEncSumm+" CURR_CODE: "+810);
				}
				JdbcUtils.close(result);
				JdbcUtils.close(pstmt);
				item.setEncSummCurr(Double.toString(currentEncSumm));
				item.setEncSummCurrCode(810);
				logger.debug("Converted Encashments: ATM: "+item.getAtmID()+" CURR_SUMM: "+item.getEncSummCurr()+" CURR_CODE: "+item.getEncSummCurrCode());
			}
			
		} catch (SQLException e) {
			logger.error("ERROR while getting ATMs encashment sums for calculation. Region ID: "+filter.getRegion()+" Date: "+filter.getDateStart(),e);
		} finally {
	    	JdbcUtils.close(result);
			JdbcUtils.close(pstmt);
	    }
	}
	
	protected static boolean checkBasicConstraints(Matrix matrix){//false if basic constraints aren't satisfied
		//check for max ATMs
		if (matrix.ENC.length-1>matrix.maxCars*matrix.MaxATMInWay){
			matrix.constraintsStatus=Matrix.CARS_OR_ATMS_CONSTRAINT_VIOLATION;
			return false;
		}
				
		//check for max money
		int currentMoney=0;
		for (int i = 1; i < matrix.ENC.length; i++) {
			currentMoney+=matrix.amountOfMoney[i];
		}
		if (currentMoney>matrix.MaxMoney*matrix.maxCars){
			matrix.constraintsStatus=Matrix.SUMM_CONSTRAINT_VIOLATION;
			return false;
		}
		
		return true;
	}
	
protected static boolean checkBasicConstraintsForSingleRoute(Matrix matrix){//false if basic constraints aren't satisfied
		//check for max ATMs
		if (matrix.ENC.length-1>matrix.MaxATMInWay){
			matrix.constraintsStatus=Matrix.CARS_OR_ATMS_CONSTRAINT_VIOLATION;
			return false;
		}
		
		//check for max money
		int currentMoney=0;
		for (int i = 1; i < matrix.ENC.length; i++) {
			currentMoney+=matrix.amountOfMoney[i];
		}
		if (currentMoney>matrix.MaxMoney){
			matrix.constraintsStatus=Matrix.SUMM_CONSTRAINT_VIOLATION;
			return false;
		}
		
		return true;
	}
	
	protected static void fillAtmTimeWindows(Connection con, AtmRouteFilter filter, Matrix matrix){
		if (matrix.getTimeWindows()!=null){
			if (matrix.getTimeWindows().size()>0) {
				matrix.removeAtmTimeWindows();
			}
		}
		
		matrix.addTimeWindow(0,0, 10000, false);//dummy window for depot
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Map<Integer,Integer> calendarDayList = null;
		String query =
			"SELECT " +
					"to_char(cald.cl_date, 'HH24') as curr_hour, " +
					"cald.CL_DAY_ENC_AVAIL as enc_avail  " +
			"FROM T_CM_CALENDAR cal, T_CM_CALENDAR_DAYS cald "+
            "where cal.cl_id=cald.cl_id and trunc(cald.cl_date)= ? and " +
            	"cal.cl_id in (select calendar_id from T_CM_ATM where atm_id=?) and cal.cl_type=? "+
            "order by curr_hour";

		try {
			logger.debug("ATM length:"+matrix.ATM.length);
			for (int currentATM = 1 ; currentATM<matrix.ATM.length; currentATM++){
				
				calendarDayList = new HashMap<Integer,Integer>();
				pstmt = con.prepareStatement(query);
				pstmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
				pstmt.setString(2, matrix.ATM[currentATM]);
				pstmt.setInt(3, 5);
	        	rs = pstmt.executeQuery();
	        	while (rs.next()){
					calendarDayList.put(Integer.valueOf(rs.getString("curr_hour")), rs.getInt("enc_avail"));
				}
				JdbcUtils.close(rs);
				JdbcUtils.close(pstmt);

				long timeZoneOffset = AtmInfoController.getTimeZoneForAtm(con,Integer.valueOf(matrix.ATM[currentATM])).getOffsetFromLocal((new Date()).getTime());
				int offsetInMinutes = (int)(timeZoneOffset/60000L);
				int currentBeginHour=-1;
				int currentEndHour=-1;
				int windowCount=0;
				int endHour = getEndHour(con, filter.getDateStart(), matrix.ATM[currentATM]);
				if (matrix.windowMode==Matrix.DEFAULT_ATM_WINDOW_MODE){
					//matrix.removeAtmTimeWindows();
					//matrix.removeTimeWindowsForAtm(currentATM-1);
					endHour=23;
				}
				//boolean windowCreated = false;
				for (int i=1; i<=endHour; i++){//23
						if (CmUtils.getNVLValue(calendarDayList.get(i-1),1) == 0 && CmUtils.getNVLValue(calendarDayList.get(i),1) ==1 ){
							currentBeginHour=i;
						}
						if (currentBeginHour>-1 && (CmUtils.getNVLValue(calendarDayList.get(i-1),1)==1 && CmUtils.getNVLValue(calendarDayList.get(i-1),1)==0 || i==endHour)){
							currentEndHour=i;
							windowCount++;
							/*if(currentBeginHour==currentEndHour)  {
								matrix.addTimeWindow(currentATM,currentBeginHour*60, (currentEndHour+1)*60);
								logger.debug("Adding time window:  Current ATM: "+currentATM+"  Begin: "+currentBeginHour+" End: "+(currentEndHour+1));
							} else {
								matrix.addTimeWindow(currentATM,currentBeginHour*60, currentEndHour*60);
								logger.debug("Adding time window:  Current ATM: "+currentATM+"  Begin: "+currentBeginHour+" End: "+currentEndHour);
							}*/
								 
							//windowCreated=true;
							if(currentBeginHour>=currentEndHour)  {
								matrix.addTimeWindow(currentATM,currentBeginHour*60+offsetInMinutes, (currentBeginHour+2)*60+offsetInMinutes, true);
								logger.debug("End of time window is before start! Adding emergency window:  Replenishment ID: "+matrix.ATM[currentATM]+"  Begin: "+currentBeginHour+" End: "+(currentBeginHour+2));
							} else {
								matrix.addTimeWindow(currentATM,currentBeginHour*60+offsetInMinutes, currentEndHour*60+offsetInMinutes, false);
								logger.debug("Adding time window:  Replenishment ID: "+matrix.ATM[currentATM]+"  Begin: "+currentBeginHour+" End: "+currentEndHour);
							}
							
							currentBeginHour=-1;
							currentEndHour=-1;
						}
					
				}
				
//				if (!windowCreated){
//					matrix.addTimeWindow(currentATM,60, 23*60);
//				}
				
				if (windowCount==0){
					if(1>=endHour)  {
						matrix.addTimeWindow(currentATM,60+offsetInMinutes, 2*60+offsetInMinutes, true);
						logger.debug("End of default time window is before start! Setting shortest avaliable window");
						logger.debug("Adding default emergency time window: Replenishment id: "+matrix.ATM[currentATM]+"  Begin: "+1+" End: "+2);
					} else {
						matrix.addTimeWindow(currentATM,60+offsetInMinutes, endHour*60+offsetInMinutes, false);
						logger.debug("Adding default time window:  Replenishment ID: "+matrix.ATM[currentATM]+"  Begin: "+1+" End: "+endHour);
					}
					
				}
	        }
			logger.debug("Emegrency time windows count: "+matrix.getEmergencyTimeWindowsCount());
			logger.debug("Adjusting emergency time windows");
			//matrix.adjustEmergencyTimeWindows();
			
        } catch (SQLException e) {
        	logger.error("Calendar load ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
	}
	
	private static int getEndHour(Connection connection, Date routeDate, String atmId){
		Calendar cal = Calendar.getInstance();
		cal.setTime(routeDate);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		
		Date encDate = getFirstEncNaPeriodBetweenDates(connection,atmId,getForthcomingEncDate(connection, atmId, cal.getTime()) , cal.getTime());
		
		cal.setTime(CmUtils.getMinValue(encDate, CmUtils.getMinValue( 
				CheckerController.getDateOutOfCashOut(connection, Integer.valueOf(atmId)),
				CheckerController.getDateOutOfCashIn(connection, Integer.valueOf(atmId)))));
		return cal.get(Calendar.HOUR_OF_DAY);
	}
	
	private static Date getForthcomingEncDate(Connection connection,
			String atmId, Date encDate){
		ResultSet rs = null;
		PreparedStatement prep = null;
		
		try {

			String sql = "select  "
					+ "DATE_FORTHCOMING_ENCASHMENT  "
					+ "from  "
					+ "t_cm_enc_plan "
					+ "where  "
					+ "ATM_ID = ? "
					+ "and trunc(DATE_FORTHCOMING_ENCASHMENT) = ? " +
					"ORDER BY DATE_FORTHCOMING_ENCASHMENT desc";
			prep = connection.prepareStatement(sql.toString());
			prep.setString(1, atmId);
			prep.setDate(2, JdbcUtils.getSqlDate(encDate));
		
			rs = prep.executeQuery();

			if (rs.next()) {
				return rs.getTimestamp("DATE_FORTHCOMING_ENCASHMENT");
			}
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			JdbcUtils.close(prep);
			JdbcUtils.close(rs);
		}
		return encDate;		
	}
	
	private static Date getFirstEncNaPeriodBetweenDates(Connection connection,
			String atmId, Date dateStart, Date dateFinish) {
		ResultSet rs = null;
		PreparedStatement prep = null;

		try {

			String sql = "select  "
					+ "DATE_START as DATE_START  "
					+ "from  "
					+ "v_cm_enc_na_days "
					+ "where  "
					+ "CL_ID = (SELECT CALENDAR_ID FROM T_CM_ATM WHERE ATM_ID = ?)  "
					+ "and DATE_START BETWEEN ? AND ? " +
					"ORDER BY DATE_START";
			prep = connection.prepareStatement(sql.toString());
			prep.setString(1, atmId);
			prep.setTimestamp(2, new Timestamp(dateStart.getTime()));
			prep.setTimestamp(3, new Timestamp(dateFinish.getTime()));
			rs = prep.executeQuery();

			if (rs.next()) {
				return rs.getTimestamp("DATE_START");
			}
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			JdbcUtils.close(prep);
			JdbcUtils.close(rs);
		}
		return dateFinish;
	}
	
	protected static void fillRiderTimeWindows(Connection con, AtmRouteFilter filter, Matrix matrix){
		String sql = "Select to_number(to_char(CL_DATE, 'HH24')) as curr_hour from T_CM_ROUTE_ORG_CALENDAR where trunc(CL_DATE) = ? and ORG_ID = ?";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		Map<Integer,Integer> calendarDayList = null;
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setInt(2, filter.getRegion());
			rs = pstmt.executeQuery();
			calendarDayList = new HashMap<Integer,Integer>();
			for (int i=1; i<24; i++){
				calendarDayList.put(i, 0);
			}
			while (rs.next()){
				calendarDayList.put(rs.getInt("curr_hour"), 1);
			}
			
			int currentBeginHour=-1;
			int currentEndHour=-1;
			int windowCount=0;
			for (int i=1; i<=23; i++){
					if (CmUtils.getNVLValue(calendarDayList.get(i-1),1) == 1 && CmUtils.getNVLValue(calendarDayList.get(i),1) ==0 ){
						currentBeginHour=i;
					}
					if (currentBeginHour>-1 && (CmUtils.getNVLValue(calendarDayList.get(i-1),1)==0 && CmUtils.getNVLValue(calendarDayList.get(i),1)==1 || i==23)){
						currentEndHour=i;
						windowCount++;
						matrix.addRiderTimeWindow(currentBeginHour*60, currentEndHour*60);
						logger.debug("Adding Rider time window:  Begin: "+currentBeginHour+" End: "+currentEndHour);
						currentBeginHour=-1;
						currentEndHour=-1;
					}
				
			}
			
			if (windowCount==0){
				matrix.addRiderTimeWindow(60, 23*60);
			}
		} catch (SQLException e) {
			logger.error("Organisation Calendar load ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
	}
	
	private static Matrix fillMatrix(Connection con ,AtmRouteFilter filter, Matrix matrix, ArrayList<AtmEncashmentItem> atms){
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		for (int i=0;i<atms.size();i++)
        {
            matrix.ATM[i] = String.valueOf(atms.get(i).getAtmID());
            matrix.ENC[i] = atms.get(i).getEncPlanID();
            if (matrix.FixPrice<0){
            	if (atms.get(i).getEncType().getId()==3){
            		matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_OUT))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==2){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_IN))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==1){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_BOTH_IN_OUT))),"9999").doubleValue();
				}
            } else {
            	Arrays.fill(matrix.AtmPrice, matrix.FixPrice);
            }
            
            matrix.amountOfMoney[i] =  (int) CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(atms.get(i).getEncSummCurr()),"9999").doubleValue();// TODO: check for overflow (maybe cast to long)
            logger.debug("ATM: "+matrix.ENC[i]+" amountOfMoney: "+matrix.amountOfMoney[i]);
        }
		
		matrix.depot=String.valueOf(atms.get(0).getAtmID());
		logger.debug("Depot: "+matrix.depot);
        
        for (int i=0;i<atms.size();i++)
        {
            for (int j=0;j<atms.size();j++)
            {
                matrix.distanceCoeffs[i][j]=i==j ? 0 : 5000;
            }
        }
        
		String sql = 
				"select pida,pidb,time, distance, 1 as POINT_ORD_A, 1 AS POINT_ORD_B "+
		        "from T_CM_ROUTE_NODES arn "+ 
		        "where   "+
	                "arn.pida in (  "+
	                     "select af.atm_id "+  
	                     "from T_CM_ENC_PLAN af   "+
	                     "join T_CM_ROUTE_ATM2ORG agr on (agr.atm_id = af.atm_id and agr.org_id = ?)  "+ 
	                     "where trunc(af.DATE_FORTHCOMING_ENCASHMENT) = ?  "+
	                     "and af.ENCASHMENT_TYPE != 0 "+
	                     "and af.ATM_ID not in " +
	                     	"(select DISTINCT  T_CM_ENC_PLAN.ATM_ID " +
	                     	"from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN " +
	                     	"where T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ROUTE_POINT.POINT_SRC_ID and T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID " +
	                     	"and T_CM_ROUTE.ROUTE_STATUS>1 AND T_CM_ROUTE.ROUTE_DATE=?) " +//check for approved and in-progress routes
	                     ")  "+
	               "and arn.pidb in (  "+
	                    "select af.atm_id  "+
	                    "from T_CM_ENC_PLAN af "+
	                    "join T_CM_ROUTE_ATM2ORG agr on (agr.atm_id = af.atm_id and agr.org_id = ?) "+
	                    "where trunc(af.DATE_FORTHCOMING_ENCASHMENT) =  ? "+
	                    "and af.ENCASHMENT_TYPE != 0 "+
	                    "and af.ATM_ID not in " +
	                     	"(select DISTINCT T_CM_ENC_PLAN.ATM_ID " +
	                     	"from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN " +
	                     	"where T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ROUTE_POINT.POINT_SRC_ID and T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID " +
	                     	"and T_CM_ROUTE.ROUTE_STATUS>1 AND T_CM_ROUTE.ROUTE_DATE=?) " +//check for approved and in-progress routes
	                    ")  "+
	            "union  all "+
	            "select pida,pidb,time, distance, 0 as POINT_ORD_A, 0 AS POINT_ORD_B "+
	            "from T_CM_ROUTE_NODES arn  "+
	            "where   "+
	                "arn.pida in (select depot_id from T_CM_ROUTE_ORG where ID=?) "+ 
	                "and arn.pidb in (  "+
	                  "select af.atm_id  "+
	                  "from T_CM_ENC_PLAN af "+  
	                  "join T_CM_ROUTE_ATM2ORG agr on (agr.atm_id = af.atm_id and agr.org_id = ?) "+ 
	                  "where trunc(af.DATE_FORTHCOMING_ENCASHMENT) =  ? "+
	                  "and af.ENCASHMENT_TYPE != 0 "+
	                  "and af.ATM_ID not in " +
                   	  "(select DISTINCT T_CM_ENC_PLAN.ATM_ID " +
                   	  "from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN " +
                   	  "where T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ROUTE_POINT.POINT_SRC_ID and T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID " +
                   	  "and T_CM_ROUTE.ROUTE_STATUS>1 AND T_CM_ROUTE.ROUTE_DATE=?) " +//check for approved and in-progress routes
	                  ")  "+
	            "union  all "+
	            "select pida,pidb,time, distance, 1 as POINT_ORD_A , 0 AS POINT_ORD_B "+
	            "from T_CM_ROUTE_NODES arn  "+
	            "where   "+
	                "arn.pidb in (select depot_id from T_CM_ROUTE_ORG where ID=?) "+ 
	                "and arn.pida in (  "+
	                "select af.atm_id  "+
	                "from T_CM_ENC_PLAN af "+  
	                "join T_CM_ROUTE_ATM2ORG agr on (agr.atm_id = af.atm_id and agr.org_id = ?) "+ 
	                "where trunc(af.DATE_FORTHCOMING_ENCASHMENT) =  ? "+
                    "and af.ENCASHMENT_TYPE != 0 "+
	                "and af.ATM_ID not in " +
	                	"(select DISTINCT T_CM_ENC_PLAN.ATM_ID " +
	                	"from T_CM_ROUTE, T_CM_ROUTE_POINT, T_CM_ENC_PLAN " +
	                	"where T_CM_ENC_PLAN.ENC_PLAN_ID=T_CM_ROUTE_POINT.POINT_SRC_ID and T_CM_ROUTE.ID=T_CM_ROUTE_POINT.ROUTE_ID " +
	                	"and T_CM_ROUTE.ROUTE_STATUS>1 AND T_CM_ROUTE.ROUTE_DATE=?) " +//check for approved and in-progress routes
	                ") "+
	                
	            "ORDER BY POINT_ORD_A,  pida, POINT_ORD_B, pidb ";
				        
		
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1,filter.getRegion());
			pstmt.setDate(2, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setDate(3, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setInt(4,filter.getRegion());
			pstmt.setDate(5, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setDate(6, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setInt(7, filter.getRegion());
			pstmt.setInt(8,filter.getRegion());
			pstmt.setDate(9, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setDate(10, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setInt(11,filter.getRegion());
			pstmt.setInt(12,filter.getRegion());
			pstmt.setDate(13, JdbcUtils.getSqlDate(filter.getDateStart()));
			pstmt.setDate(14, JdbcUtils.getSqlDate(filter.getDateStart()));
			
			rs = pstmt.executeQuery(); 
			
			int n=atms.size();;
			for (int i = 0; i<n; i++)
			{
				for (int j = 0; j<n; j++)
				{
					if (i!=j)
					{	if (rs.next()){
							
							matrix.distanceCoeffs[i][j]=(int)(rs.getInt("distance")/1000);
							matrix.timeCoeffs[i][j]=(int)(rs.getInt("time")/60);
							logger.debug("PIDA: "+rs.getString("PIDA")+" PIDB: "+rs.getString("PIDB")+" i: "+i+" j: "+j+" c: "+matrix.distanceCoeffs[i][j]);
						}
					}
				}
			}
			
			return matrix;
		} catch (SQLException e) {
			logger.error("Matrix load ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
		return null;
	}
	
	private static Matrix fillMatrixFromRoute(Connection con ,AtmRouteFilter filter, Matrix matrix, ArrayList<AtmEncashmentItem> atms, int routeN){
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		for (int i=0;i<atms.size();i++)
        {
			matrix.ATM[i] = String.valueOf(atms.get(i).getAtmID());
            matrix.ENC[i] = atms.get(i).getEncPlanID();
            if (matrix.FixPrice<0){
            	if (atms.get(i).getEncType().getId()==3){
            		matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_OUT))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==2){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_IN))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==1){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_BOTH_IN_OUT))),"9999").doubleValue();
				}
            } else {
            	Arrays.fill(matrix.AtmPrice, matrix.FixPrice);
            }
            matrix.amountOfMoney[i] =  (int) CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(atms.get(i).getEncSummCurr()),"9999").doubleValue();// TODO: ��������� �� ������������ (�������� ���������� ��� long)
            logger.debug("ATM: "+matrix.ENC[i]+" amountOfMoney: "+matrix.amountOfMoney[i]);
        }
		
		matrix.depot=String.valueOf(atms.get(0).getAtmID());
		logger.debug("Depot: "+matrix.depot);
        
        for (int i=0;i<atms.size();i++)
        {
            for (int j=0;j<atms.size();j++)
            {
                matrix.distanceCoeffs[i][j]=i==j ? 0 : 5000;
            }
        }
        
        String sql = 
				"select pida,pidb,time, distance, 1 as POINT_ORD_A, 1 AS POINT_ORD_B "+
		        "from T_CM_ROUTE_NODES arn "+ 
		        "where   "+
		            "arn.pida in (  "+
			            "select ATM_ID  "+
				        "from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) where route_id=? "+
		                 ")  "+
		           "and arn.pidb in (  "+
		           		"select ATM_ID  "+
		           		"from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) where route_id=? "+
		                ")  "+
		        "union  all "+
		        "select pida,pidb,time, distance, 0 as POINT_ORD_A, 0 AS POINT_ORD_B "+
		        "from T_CM_ROUTE_NODES arn  "+
		        "where   "+
		            "arn.pida in (select depot_id from T_CM_ROUTE_ORG where ID=?) "+ 
		            "and arn.pidb in (  "+
		            	"select ATM_ID  "+
		            	"from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) where route_id=? "+
		              ")  "+
		        "union  all "+
		        "select pida,pidb,time, distance, 1 as POINT_ORD_A , 0 AS POINT_ORD_B "+
		        "from T_CM_ROUTE_NODES arn  "+
		        "where   "+
		            "arn.pidb in (select depot_id from T_CM_ROUTE_ORG where ID=?) "+ 
		            "and arn.pida in (  "+
			            "select ATM_ID  "+
				        "from (select ORD,ROUTE_ID,ATM_ID from T_CM_ROUTE_POINT, T_CM_ENC_PLAN where T_CM_ROUTE_POINT.POINT_SRC_ID=T_CM_ENC_PLAN.ENC_PLAN_ID) where route_id=? "+
		            ") "+
		            
		        "ORDER BY POINT_ORD_A,  pida, POINT_ORD_B, pidb ";
		
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1,routeN);
			pstmt.setInt(2,routeN);
			pstmt.setInt(3,filter.getRegion());
			pstmt.setInt(4,routeN);
			pstmt.setInt(5,filter.getRegion());
			pstmt.setInt(6,routeN);
			rs = pstmt.executeQuery(); 
			
			int n=atms.size();;
			for (int i = 0; i<n; i++)
			{
				for (int j = 0; j<n; j++)
				{
					if (i!=j)
					{	if (rs.next()){
							
							matrix.distanceCoeffs[i][j]=(int)(rs.getInt("distance")/1000);
							matrix.timeCoeffs[i][j]=(int)(rs.getInt("time")/60);
							logger.debug("PIDA: "+rs.getString("PIDA")+" PIDB: "+rs.getString("PIDB")+" i: "+i+" j: "+j+" c: "+matrix.distanceCoeffs[i][j]);
						}
					}
				}
			}
			
			return matrix;
		} catch (SQLException e) {
			logger.error("Matrix from points load ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
		return null;
	}
	
	private static Matrix fillMatrixFromInProgressRoute(Connection con ,AtmRouteFilter filter, Matrix matrix, ArrayList<AtmEncashmentItem> atms, int routeN){
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		for (int i=0;i<atms.size();i++)
        {
			matrix.ATM[i] = String.valueOf(atms.get(i).getAtmID());
            matrix.ENC[i] = atms.get(i).getEncPlanID();
            if (matrix.FixPrice<0){
            	if (atms.get(i).getEncType().getId()==3){
            		matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_OUT))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==2){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_CASH_IN))),"9999").doubleValue();
				} else if (atms.get(i).getEncType().getId()==1){
					matrix.AtmPrice[i]=CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(Double.valueOf(getAtmAttribute(con, atms.get(i).getAtmID(), AtmAttribute.ENC_COST_BOTH_IN_OUT))),"9999").doubleValue();
				}
            } else {
            	Arrays.fill(matrix.AtmPrice, matrix.FixPrice);
            }
            matrix.amountOfMoney[i] =  (int) CmCommonController.convertValue(con, atms.get(i).getEncSummCurrCode(), matrix.currCode,Double.valueOf(atms.get(i).getEncSummCurr()),"9999").doubleValue();// TODO: ��������� �� ������������ (�������� ���������� ��� long)
            logger.debug("ATM: "+matrix.ENC[i]+" amountOfMoney: "+matrix.amountOfMoney[i]);
        }
		
		matrix.depot=String.valueOf(atms.get(0).getAtmID());
		logger.debug("Depot: "+matrix.depot);
        
        for (int i=0;i<atms.size();i++)
        {
            for (int j=0;j<atms.size();j++)
            {
                matrix.distanceCoeffs[i][j]=i==j ? 0 : 5000;
            }
        }
        
        String sql = 
        		"select pida,pidb,time, distance, 1 as POINT_ORD_A, 1 AS POINT_ORD_B "+
        		          "from T_CM_ROUTE_NODES arn "+ 
        		          "where   "+
        		              "arn.pida in (  " +
        		               "select atm_id from t_cm_enc_plan where enc_plan_id in ( "+
        		                "select POINT_SRC_ID  "+
        		             "from T_CM_ROUTE_POINT where route_id=? )"+
        		                   ")  "+
        		             "and arn.pidb in (  "+
        		               "select atm_id from t_cm_enc_plan where enc_plan_id in ( "+
        		                "select POINT_SRC_ID  "+
        		                "from T_CM_ROUTE_POINT where route_id=? )"+
        		                  ")  "+
        		          "union  all "+
        		          "select 0 as pida,pidb,time, distance, 0 as POINT_ORD_A, 0 AS POINT_ORD_B "+
        		          "from T_CM_ROUTE_NODES arn  "+
        		          "where   "+
        		              "arn.pida = ?"+ 
        		              "and arn.pidb in (  "+
        		               "select atm_id from t_cm_enc_plan where enc_plan_id in ( "+
        		                "select POINT_SRC_ID  "+
        		                "from T_CM_ROUTE_POINT where route_id=? )"+
        		                ")  "+
        		          "union  all "+
        		          "select pida,0 as pidb,time, distance, 1 as POINT_ORD_A , 0 AS POINT_ORD_B "+
        		          "from T_CM_ROUTE_NODES arn  "+
        		          "where   "+
        		              "arn.pidb =? "+ 
        		              "and arn.pida in (  "+
        		               "select atm_id from t_cm_enc_plan where enc_plan_id in ( "+
        		                "select POINT_SRC_ID  "+
        		                "from T_CM_ROUTE_POINT where route_id=? )"+
        		              ") "+
        		              
        		          "ORDER BY POINT_ORD_A,  pida, POINT_ORD_B, pidb ";
		
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1,routeN);
			pstmt.setInt(2,routeN);
			pstmt.setInt(3,atms.get(0).getAtmID());
			pstmt.setInt(4,routeN);
			pstmt.setInt(5,atms.get(0).getAtmID());
			pstmt.setInt(6,routeN);
			rs = pstmt.executeQuery(); 
			
			int n=atms.size();;
			for (int i = 0; i<n; i++)
			{
				for (int j = 0; j<n; j++)
				{
					if (i!=j)
					{	if (rs.next()){
							
							matrix.distanceCoeffs[i][j]=(int)(rs.getInt("distance")/1000);
							matrix.timeCoeffs[i][j]=(int)(rs.getInt("time")/60);
							logger.debug("PIDA: "+(rs.getString("PIDA")=="0" ? atms.get(0).getAtmID() : rs.getString("PIDA"))+" PIDB: "+(rs.getString("PIDB")=="0" ? atms.get(0).getAtmID() : rs.getString("PIDB"))+" i: "+i+" j: "+j+" c: "+matrix.distanceCoeffs[i][j]);
						}
					}
				}
			}
			
			return matrix;
		} catch (SQLException e) {
			logger.error("Matrix from points from In Progress Route load ERROR",e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
		return null;
	}
	
	protected static void setOrgParametersForRouting(Connection con, Matrix matrix, int orgID) throws RoutingException{
		List<AtmGroupAttributeItem> attrList = OrgController.getAttributeListForOrg(con, orgID);
		
        Arrays.fill(matrix.AtmPrice, 100);// TODO: different encashment price for different atms, consider using this parameter later
        Arrays.fill(matrix.amountOfCassettes,200);// TODO: consider using this parameter later
        matrix.amountOfCassettes[0]=0;
		
		for (AtmGroupAttributeItem item: attrList){
			if (item.isUsed()){
				if (item.getAttributeID()==OrgAttribute.MAX_GROUP_CARS.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						matrix.maxCars=Integer.valueOf(item.getValue());
					else 
						matrix.maxCars=8;
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.MAX_CAR_ENC.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty()){
						if (Integer.valueOf(item.getValue())>0){
							matrix.MaxATMInWay=Integer.valueOf(item.getValue());
						}
						else 
							throw new RoutingException(2);
					}
					else
						matrix.MaxATMInWay=8;
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.MAX_LOAD_SUMM.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty()){
						if (Integer.valueOf(item.getValue())>0){
							matrix.MaxMoney=Integer.valueOf(item.getValue());
							matrix.VolumeOneCar=matrix.MaxMoney;
						}
						else 
							throw new RoutingException(2);
					}
					else {
						matrix.MaxMoney=40000000;
						matrix.VolumeOneCar=matrix.MaxMoney;
					}
						
						//throw new RoutingException(1);	
				}
				if (item.getAttributeID()==OrgAttribute.MAX_LENGTH.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty()){
						if (Integer.valueOf(item.getValue())>0){
							matrix.MaxLength=Integer.valueOf(item.getValue());
						}
						else 
							throw new RoutingException(2);
					}
					else 
						matrix.MaxLength=600000;
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.MAX_TIME.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty()){
						if (Integer.valueOf(item.getValue())>0){
							matrix.MaxTime=Integer.valueOf(item.getValue());
						}
						else 
							throw new RoutingException(2);
					}
					else 
						matrix.MaxTime=600;
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.KILOMETER_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty() && item.isUsed()){
						if (Double.valueOf(item.getValue())>0){
							matrix.LengthPrice=Double.valueOf(item.getValue());
						}
						else if (Double.valueOf(item.getValue())==0){
							matrix.LengthPrice=20; //Consider handling zero length in core
						} else {
							throw new RoutingException(2);
						}
					}
					else 
						matrix.LengthPrice=20;
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.FIX_ENC_COST.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty() && item.isUsed()){
						if (Double.valueOf(item.getValue())>0){
							matrix.FixPrice=Double.valueOf(item.getValue());
						}
						else 
							throw new RoutingException(2);
					}
					else 
						matrix.FixPrice=-1;//1500
						//throw new RoutingException(1);
				}
				if (item.getAttributeID()==OrgAttribute.TECHNICAL_TIME.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty()){
						if (Integer.valueOf(item.getValue())>0){
							Arrays.fill(matrix.serviceTime, Integer.valueOf(item.getValue()));
						}
						else 
							throw new RoutingException(2);
					}
					else 
						Arrays.fill(matrix.serviceTime, 20);
						//throw new RoutingException(1);	
				}
				if (item.getAttributeID()==OrgAttribute.CURRENCY_CODE.getId()){
					if (item.getValue()!=null && !item.getValue().isEmpty())
						matrix.currCode=Integer.valueOf(item.getValue());
					else 
						throw new RoutingException(1);
						
				}

				logger.debug("ID: "+item.getAttributeID()+" DESCX: "+item.getDescx()+" Value: "+item.getValue());
			}
		}
		
	}

	protected static void checkTimeWindows(Matrix matrix) throws RoutingException {
		int correctWindowCount;
		for (TimeWindow currentAtmWindow : matrix.getTimeWindows()){
			correctWindowCount = 0;
			for (RiderTimeWindow riderWindow : matrix.getRiderTimeWindows()){
				if (!(riderWindow.StartWork>currentAtmWindow.StartWork && riderWindow.StartWork>=currentAtmWindow.EndWork)
						&& !(riderWindow.EndWork<=currentAtmWindow.StartWork && riderWindow.EndWork<currentAtmWindow.EndWork)){
					correctWindowCount++;
				}
			}
			if (correctWindowCount==0){
				throw new RoutingException(RoutingException.INCORRECT_PARAMETERS);
			}
			
		}
		
	}
	
	protected static String getAtmAttribute(Connection con, int atmId, AtmAttribute attr) {
		Map<Integer, String> atmAttributes;
		try {
			atmAttributes = CmCommonController.getAtmAttributes(con, atmId);
			String attributeValue = atmAttributes.get(attr.getAttrID());
			if(attributeValue == null){
				if(attr.isRequired()){
					throw new ForecastException(ForecastException.ATM_ATTRIBUTE_NOT_DEFINED);
				} else {
					attributeValue = "0";
				}
			}
			return attributeValue;
		} catch (ForecastException e) {
			if (e.getCode()==11){
				logger.error("Atm attribute not defined");
				return "";
			} else {
				logger.error("Error while getting atm attributes");
			return "";
			}
		}
		
	}
}
