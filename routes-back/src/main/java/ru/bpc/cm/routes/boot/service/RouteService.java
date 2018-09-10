package ru.bpc.cm.routes.boot.service;

import ru.bpc.cm.items.routes.AtmLocationItem;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRouteItem;
import ru.bpc.cm.items.routes.RoutingException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface RouteService {

    ArrayList<AtmRouteItem> getRoutes(AtmRouteFilter filter);

    void addRoute(AtmRouteFilter filter);

    void deleteRoute(int routeId, AtmRouteFilter filter);

    AtmRouteItem approveRoute(int routeId);

    List<AtmLocationItem> checkMatrixByRegionDate(int region, Date routingDate);

    void calculateOptimalRoute(String uid, AtmRouteFilter filter) throws RoutingException;

    void getPidsForMatrix(List<AtmLocationItem> atms, int regionId);

    AtmRouteItem recalculateRoute(AtmRouteFilter filter, int routeId) throws RoutingException;
}
