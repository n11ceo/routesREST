package ru.bpc.cm.routes.boot.dao;

import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRoutePointItem;

import java.util.ArrayList;

public interface RoutePointDao {
    ArrayList<AtmRoutePointItem> getPoints(int routeN);

    ArrayList<AtmRoutePointItem> getPoints(AtmRouteFilter filter);

    void updateAtmPointGeo(String city, String street, String latitude, String longitude);

    void updateDepotPointGeo(String city, String street, String latitude, String longitude);
}
