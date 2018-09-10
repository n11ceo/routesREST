package ru.bpc.cm.routes.boot.service;

import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRoutePointItem;
import ru.bpc.cm.items.routes.DepotItem;

import java.util.ArrayList;

public interface RoutePointService {
    ArrayList<AtmRoutePointItem> getPoints(int routeN);

    ArrayList<AtmRoutePointItem> getPoints(AtmRouteFilter filter);

    ArrayList<AtmRoutePointItem> geocodeAtmPoints(ArrayList<AtmRoutePointItem> points) throws Exception;

    ArrayList<DepotItem> geocodeDepotPoints(ArrayList<DepotItem> points) throws Exception;
}
