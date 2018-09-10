package ru.bpc.cm.routes.boot.dao;

import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.items.routes.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface RouteDao {
    ArrayList<AtmRouteItem> getRoutes(AtmRouteFilter filter);

    void createRoute(AtmRouteFilter filter);

    void deleteRoute(int routeId, AtmRouteFilter filter);

    AtmRouteItem getRoute(int routeN);

    void updateRouteStatus(int routeId, RouteStatus status);

    List<AtmLocationItem> getMatrixByRegionDate(int region, Date routingDate);

    void recalculateRoute(AtmRouteFilter filter, int routeId) throws RoutingException;

    ArrayList<MatrixCoordItem> getPids(int region, String atmString);

    void createRouteMatrix(ArrayList<MatrixCoordItem> matrixPidList, String region);

    void calculateRoutesGenetic(String uid, AtmRouteFilter filter) throws RoutingException;
}
