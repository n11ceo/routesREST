package ru.bpc.cm.routes.boot.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bpc.cm.cashmanagement.CmCommonController;
import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.items.routes.*;
import ru.bpc.cm.routes.RoutingController;
import ru.bpc.cm.routes.RoutingUtils;
import ru.bpc.cm.utils.db.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository("routeDao")
public class RouteDaoImpl implements RouteDao {

    private Logger slf4jLog = LoggerFactory.getLogger(RouteDaoImpl.class);

    private DataSource dataSource;

    @Autowired
    public RouteDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ArrayList<AtmRouteItem> getRoutes(AtmRouteFilter filter) {
        ArrayList<AtmRouteItem> routes = new ArrayList<AtmRouteItem>();
        Connection connection = null;
        try {
            slf4jLog.debug("Trying to get list of routes on" + filter.getDateStart());
            connection = dataSource.getConnection();
            routes = RoutingController.getRoutes(connection, filter);
        } catch (Exception e) {
            slf4jLog.error("Unhandeled exception while getting Routes", e);
        } finally {
            JdbcUtils.close(connection);
        }
        slf4jLog.debug("Returning list of routes" + routes.size());
        return routes;
    }

    @Override
    public void createRoute(AtmRouteFilter filter) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            slf4jLog.debug("Trying to add Route");
            String query = "insert into T_CM_ROUTE " + "(ID, ROUTE_DATE, ORG_ID, ROUTE_STATUS) "
                    + "values (" + JdbcUtils.getNextSequence(connection, "SQ_CM_ROUTE") + ", ?, ?, 1)";
            pstmt = connection.prepareStatement(query);
            pstmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
            pstmt.setInt(2, filter.getRegion());

            pstmt.execute();
        } catch (Exception e) {
            slf4jLog.error("Error while Adding Route", e);
        } finally {
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
    }

    @Override
    public void deleteRoute(int routeId, AtmRouteFilter filter) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            slf4jLog.debug("Trying to delete Route");
            String query = "delete from T_CM_ROUTE_POINT where ROUTE_ID=? ";
            pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, routeId);
            pstmt.executeUpdate();
            JdbcUtils.close(pstmt);

            query = "delete from T_CM_ROUTE " +
                    " where ROUTE_DATE=? and ORG_ID=? and ID=? ";
            pstmt = connection.prepareStatement(query);
            pstmt.setDate(1, JdbcUtils.getSqlDate(filter.getDateStart()));
            pstmt.setInt(2, filter.getRegion());
            pstmt.setInt(3, routeId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            slf4jLog.error("Error while deleting Route", e);
        } finally {
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
    }

    @Override
    public AtmRouteItem getRoute(int routeN) {
        Connection connection;
        PreparedStatement prep = null;
        ResultSet rs = null;
        AtmRouteItem atmRouteItem = null;
        String query = "select ID, ROUTE_DATE, ORG_ID , RESULT_FLAG, COST, COST_CURR, ROUTE_TIME, ROUTE_LENGTH, atmp.POINTS, " +
                "ROUTE_STATUS " +
                "from T_CM_ROUTE left outer join (select count(ORD) as POINTS, ROUTE_ID from T_CM_ROUTE_POINT group by ROUTE_ID) atmp on atmp.ROUTE_ID=T_CM_ROUTE.ID " +
                "where ID = ?";
        try {
            connection = dataSource.getConnection();
            prep = connection.prepareStatement(query);
            prep.setInt(1, routeN);
            rs = prep.executeQuery();
            while (rs.next()) {
                atmRouteItem = new AtmRouteItem(rs.getInt("ID"), rs.getInt("ORG_ID"),
                        rs.getDate("ROUTE_DATE"), false, rs.getInt("POINTS"),
                        rs.getInt("ROUTE_LENGTH"), rs.getInt("ROUTE_TIME"),
                        rs.getLong("COST"),
                        CmCommonController.getCurrCodeA3(connection, rs.getInt("COST_CURR")),
                        rs.getInt("ROUTE_STATUS"), rs.getInt("RESULT_FLAG"));
            }
        } catch (SQLException e) {
            slf4jLog.error("Routes get ERROR", e);
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(prep);
        }

        return atmRouteItem;
    }

    @Override
    public void updateRouteStatus(int routeId, RouteStatus routeStatus) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            RoutingUtils.updateRouteStatus(connection, routeId, routeStatus);
        } catch (Exception e) {
            slf4jLog.error("Error while updating Nodes distance", e);
        } finally {
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
    }

    @Override
    public List<AtmLocationItem> getMatrixByRegionDate(int region, Date routingDate) {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        ArrayList<AtmLocationItem> atms = new ArrayList<AtmLocationItem>();
        try {
            connection = dataSource.getConnection();
            String query = "select T_CM_ROUTE_ATM2ORG.ATM_ID  from T_CM_ROUTE_ATM2ORG "
                    + "where (T_CM_ROUTE_ATM2ORG.ATM_ID not in (select PIDA from T_CM_ROUTE_NODES) "
                    + "or T_CM_ROUTE_ATM2ORG.ATM_ID not in (select PIDB from T_CM_ROUTE_NODES)) "
                    + "and T_CM_ROUTE_ATM2ORG.ATM_ID in (select T_CM_ENC_PLAN.ATM_ID from T_CM_ENC_PLAN where trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT)=trunc(?)) "
                    + "and T_CM_ROUTE_ATM2ORG.ORG_ID=?";

            if (isDepotInMatrixForCurrentDate(region, routingDate)) {
                pstmt = connection.prepareStatement(query);
                pstmt.setDate(1, JdbcUtils.getSqlDate(routingDate));
                pstmt.setInt(2, region);
                slf4jLog.debug("setting region: " + region);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    AtmLocationItem item = new AtmLocationItem();
                    item.setPid(rs.getString("atm_id"));
                    item.setChecked(true);
                    atms.add(item);
                }
            } else {
                query = "select T_CM_ROUTE_ATM2ORG.ATM_ID  from T_CM_ROUTE_ATM2ORG, T_CM_ENC_PLAN "
                        + "where T_CM_ROUTE_ATM2ORG.ATM_ID=T_CM_ENC_PLAN.ATM_ID and T_CM_ROUTE_ATM2ORG.ORG_ID=? and trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT)=trunc(?)";
                pstmt = connection.prepareStatement(query);
                pstmt.setInt(1, region);
                pstmt.setDate(2, JdbcUtils.getSqlDate(routingDate));
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    AtmLocationItem item = new AtmLocationItem();
                    item.setPid(rs.getString("atm_id"));
                    item.setChecked(true);
                    atms.add(item);
                }
            }

        } catch (Exception e) {
            slf4jLog.error("Error while Checking Region Matrix", e);
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
        return atms;
    }

    @Override
    public void recalculateRoute(AtmRouteFilter filter, int routeId) throws RoutingException {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            RoutingController.recalculateRoute(connection,filter,
                    routeId);
        } catch (Exception e) {
            if (e instanceof RoutingException){
                throw (RoutingException) e;
            }
            slf4jLog.error("Unhandled calculation error",e);
        } finally {
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
    }

    @Override
    public ArrayList<MatrixCoordItem> getPids(int region, String atmString) {
        Connection con = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        ArrayList<MatrixCoordItem> loc = new ArrayList<MatrixCoordItem>();

        String query = "select table1.A as A, table1.B as B, table1.CoordA, table1.CoordB, table1.CityA, table1.CityB, table1.AddressA, "
                + "table1.AddressB, table1.distance, table1.time, table1.groupid " //, coalesce(points.cou,0) as additionalPoints
                + " from  "
                + "(select tr1.atm_id as A,ai1.latitude||','||ai1.longitude as CoordA, ai1.city as CityA, ai1.street as AddressA, "
                + "ai2.latitude||','||ai2.longitude as CoordB, ai2.city as CityB, ai2.street as AddressB, tr2.atm_id as B,tr1.org_id as groupid, "
                + "coalesce(art.distance,0) as distance, coalesce(art.time,0) as time "
                + "from T_CM_ROUTE_ATM2ORG tr1  "
                + "left outer join T_CM_ROUTE_ATM2ORG tr2 on (tr1.org_id = tr2.org_id) "
                + "join T_CM_ATM ai1 on (ai1.atm_id = tr1.atm_id) "
                + "join T_CM_ATM ai2 on (ai2.atm_id = tr2.atm_id) "
                + "left outer join T_CM_ROUTE_NODES art on (art.pida = tr1.atm_id and art.pidb = tr2.atm_id) "
                + "where tr1.atm_id <> tr2.atm_id and ((tr1.atm_id in ("
                + atmString
                + ") and tr2.atm_id not in ("
                + atmString
                + ")) or (tr1.atm_id not in ("
                + atmString
                + ") and tr2.atm_id in ("
                + atmString
                + ")) or (tr1.atm_id in ("
                + atmString
                + ") and tr2.atm_id in ("
                + atmString
                + "))) "
                + "and tr1.org_id =? "
                + "union all "
                + "select ai1.id as A,ai1.latitude||','||ai1.longitude as CoordA, ai1.city as CityA, ai1.street as AddressA, "
                + "ai2.latitude||','||ai2.longitude as CoordB, ai2.city as CityB, ai2.street as AddressB, tr2.atm_id as B,tr1.id as groupid, "
                + "coalesce(art.distance,0) as distance,  coalesce(art.time,0) as time "
                + "from T_CM_ROUTE_ORG tr1  "
                + "left outer join T_CM_ROUTE_ATM2ORG tr2 on (tr1.id = tr2.org_id) "
                + "join T_CM_ROUTE_DEPOT ai1 on (ai1.id = tr1.depot_id) "
                + "join T_CM_ATM ai2 on (ai2.atm_id = tr2.atm_id) "
                + "left outer join T_CM_ROUTE_NODES art on (art.pida = tr1.depot_id and art.pidb = tr2.atm_id) "
                + " where tr2.atm_id in ("
                + atmString
                + ") "
                + " and tr1.id =? "
                + "union all "
                + " select tr1.atm_id as A,ai1.latitude||','||ai1.longitude as CoordA, ai1.city as CityA, ai1.street as AddressA, "
                + "ai2.latitude||','||ai2.longitude as CoordB, ai2.city as CityB, ai2.street as AddressB, ai2.id as B,tr1.org_id as groupid, "
                + "coalesce(art.distance,0) as distance ,  coalesce(art.time,0) as time "
                + " from T_CM_ROUTE_ATM2ORG tr1  "
                + " left outer join T_CM_ROUTE_ORG tr2 on (tr1.org_id = tr2.id) "
                + " join T_CM_ATM ai1 on (ai1.atm_id = tr1.atm_id) "
                + " join T_CM_ROUTE_DEPOT ai2 on (ai2.id = tr2.depot_id) "
                + " left outer join T_CM_ROUTE_NODES art on (art.pida = tr1.atm_id and art.pidb = tr2.depot_id) "
                + "where tr1.atm_id in ("
                + atmString
                + ") "
                + "and tr1.org_id =? "
                + "order by A, B ) table1  "

                + "where 1=1 ";
        try {
            con = dataSource.getConnection();
            prep = (PreparedStatement) con.prepareStatement(query);
            prep.setInt(1, region);
            prep.setInt(2, region);
            prep.setInt(3, region);

            rs = prep.executeQuery();
            while (rs.next()) {
                MatrixCoordItem item = new MatrixCoordItem();
                item.setPidA(rs.getString("A"));
                item.setPidB(rs.getString("B"));
                item.setCoordA(rs.getString("CoordA"));
                item.setCoordB(rs.getString("CoordB"));
                item.setAddressA(rs.getString("AddressA"));
                item.setAddressB(rs.getString("AddressB"));
                item.setCityA(rs.getString("CityA"));
                item.setCityB(rs.getString("CityB"));
                //item.setAdditionalPoints(rs.getString("additionalPoints"));
                item.setDistance(rs.getString("Distance"));
                item.setTime(rs.getString("Time"));
                item.setGroupid(rs.getString("groupid"));
                if (item.getDistance().equals("0")) {
                    item.setNeedCalc(true);
                } else {
                    item.setNeedCalc(true);
                }
                loc.add(item);
                slf4jLog.debug(item.getPidA() + " " + " "
                        + item.getCoordA());
                slf4jLog.debug(item.getPidB() + " " + " "
                        + item.getCoordB());
            }
        } catch (Exception e) {
            slf4jLog.error("getPidsForMatrix failed", e);
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(prep);
            JdbcUtils.close(con);
        }
        return loc;
    }

    @Override
    public void createRouteMatrix(ArrayList<MatrixCoordItem> matrixPidList, String region) {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            RoutingController.saveRouteMatrix(con,
                    matrixPidList, region);
        } catch (Exception e) {
            slf4jLog.error("saveMatrix failed", e);
        } finally {
            JdbcUtils.close(con);
        }
    }

    @Override
    public void calculateRoutesGenetic(String uid, AtmRouteFilter filter) throws RoutingException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            RoutingController.calculateRoutesGenetic(connection, uid, filter);
        } catch (Exception e) {
            if (e instanceof RoutingException) {
                throw (RoutingException) e;
            }
            slf4jLog.error("Unhandled calculation error", e);
        } finally {
            JdbcUtils.close(connection);
        }
    }

    private boolean isDepotInMatrixForCurrentDate(int region, Date routingDate) {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        boolean result = true;
        try {
            connection = dataSource.getConnection();
            String query = "select T_CM_ROUTE_ORG.DEPOT_ID  from T_CM_ROUTE_ORG, T_CM_ROUTE_ATM2ORG "
                    + "where (T_CM_ROUTE_ORG.ID=T_CM_ROUTE_ATM2ORG.ORG_ID and T_CM_ROUTE_ORG.DEPOT_ID not in (select PIDA from T_CM_ROUTE_NODES) "
                    + "and T_CM_ROUTE_ORG.DEPOT_ID not in (select PIDB from T_CM_ROUTE_NODES)) "
                    + "and T_CM_ROUTE_ATM2ORG.ATM_ID in (select T_CM_ENC_PLAN.ATM_ID from T_CM_ENC_PLAN where trunc(T_CM_ENC_PLAN.DATE_FORTHCOMING_ENCASHMENT)=trunc(?))"
                    + "and T_CM_ROUTE_ORG.ID=?";
            pstmt = connection.prepareStatement(query);
            pstmt.setDate(1, JdbcUtils.getSqlDate(routingDate));
            pstmt.setInt(2, region);
            rs = pstmt.executeQuery();
            if (rs.next())
                result = false;
            else
                result = true;

        } catch (Exception e) {
            slf4jLog.error("Error while Checking if depot in Matrix", e);
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(pstmt);
            JdbcUtils.close(connection);
        }
        return result;
    }
}
