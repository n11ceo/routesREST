package ru.bpc.cm.routes.boot.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRoutePointItem;
import ru.bpc.cm.routes.RoutingController;
import ru.bpc.cm.utils.db.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

@Repository("routePointDao")
public class RoutePointDaoImpl implements RoutePointDao {

    private Logger slf4jLog = LoggerFactory.getLogger(RoutePointDaoImpl.class);

    private DataSource dataSource;

    @Autowired
    public RoutePointDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ArrayList<AtmRoutePointItem> getPoints(int routeN) {
        ArrayList<AtmRoutePointItem> points = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            points = RoutingController.getPoints(connection, routeN);
        } catch (Exception e) {
            slf4jLog.error("Unhandeled exception while getting points", e);
        } finally {
            JdbcUtils.close(connection);
        }

        return points;
    }

    @Override
    public ArrayList<AtmRoutePointItem> getPoints(AtmRouteFilter filter) {
        ArrayList<AtmRoutePointItem> points = new ArrayList<AtmRoutePointItem>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            points = RoutingController.getPoints(connection, filter);
        } catch (Exception e) {
            slf4jLog.error("Unhandeled exception while getting points", e);
        } finally {
            JdbcUtils.close(connection);
        }

        return points;
    }

    @Override
    public void updateAtmPointGeo(String city, String street, String latitude, String longitude) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            String sql = "UPDATE " +
                    "T_CM_ATM " +
                    "SET " +
                    "latitude = ?, " +
                    "longitude = ?" +
                    "WHERE CITY = ? AND STREET = ?";
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, latitude);
            pstmt.setString(2, longitude);
            pstmt.setString(3, city);
            pstmt.setString(4, street);
            pstmt.executeUpdate();
        } catch (Exception e) {
            slf4jLog.error("Unhandeled exception while getting points", e);
        } finally {
            JdbcUtils.close(connection);
        }
    }

    @Override
    public void updateDepotPointGeo(String city, String street, String latitude, String longitude) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
            String sql = "UPDATE " +
                    "T_CM_ROUTE_DEPOT " +
                    "SET " +
                    "latitude = ?, " +
                    "longitude = ?" +
                    "WHERE CITY = ? AND STREET = ?";
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, latitude);
            pstmt.setString(2, longitude);
            pstmt.setString(3, city);
            pstmt.setString(4, street);
            pstmt.executeUpdate();
        } catch (Exception e) {
            slf4jLog.error("Unhandeled exception while getting points", e);
        } finally {
            JdbcUtils.close(connection);
        }
    }
}
