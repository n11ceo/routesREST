package ru.bpc.cm.routes.boot.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bpc.cm.items.enums.RouteStatus;
import ru.bpc.cm.items.routes.*;
import ru.bpc.cm.routes.RoutingUtils;
import ru.bpc.cm.routes.boot.dao.RouteDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service("routeService")
public class RouteServiceImpl implements RouteService {

    private Logger slf4jLog = LoggerFactory.getLogger(RouteServiceImpl.class);

    private final RouteDao routeDao;

    @Autowired
    public RouteServiceImpl(RouteDao routeDao) {
        this.routeDao = routeDao;
    }

    public ArrayList<AtmRouteItem> getRoutes(AtmRouteFilter filter) {
        return routeDao.getRoutes(filter);
    }

    @Override
    public void addRoute(AtmRouteFilter filter) {
        routeDao.createRoute(filter);
    }

    @Override
    public void deleteRoute(int routeId, AtmRouteFilter filter) {
        routeDao.deleteRoute(routeId, filter);
    }

    @Override
    public AtmRouteItem approveRoute(int routeN) {
        routeDao.updateRouteStatus(routeN, RouteStatus.APPROVED);
        return routeDao.getRoute(routeN);
    }

    @Override
    public List<AtmLocationItem> checkMatrixByRegionDate(int region, Date routingDate) {
        return routeDao.getMatrixByRegionDate(region, routingDate);
    }

    @Override
    public void calculateOptimalRoute(String uid, AtmRouteFilter filter) throws RoutingException {
        switch (filter.getOptimizationType()) {
            case 1: //GENETIC
                routeDao.calculateRoutesGenetic(uid, filter);
                //TODO only GENETIC impl
        }
    }

    @Override
    public void getPidsForMatrix(List<AtmLocationItem> atms, int regionId) {
        ArrayList<MatrixCoordItem> tableData = null;
        StringBuilder atmString = new StringBuilder();
        AtomicInteger progress = new AtomicInteger(0);
        slf4jLog.debug(Integer.toString(atms.size()));
        for (AtmLocationItem item : atms) {
            if (atmString.length() > 0) {
                atmString.append(" ,").append(item.getPid());
            } else {
                atmString = new StringBuilder(item.getPid());
            }
        }
        // replaced String to String builder for atmString
        slf4jLog.debug(atmString.toString());
        tableData = routeDao.getPids(regionId, atmString.toString());
        slf4jLog.debug("PidList_size" + tableData.size());
        RoutingUtils.getDistanceMatrixForATMs(tableData, progress);
        routeDao.createRouteMatrix(tableData, Integer.toString(regionId));
    }

    @Override
    public AtmRouteItem recalculateRoute(AtmRouteFilter filter, int routeId) throws RoutingException {
        routeDao.recalculateRoute(filter, routeId);
        return routeDao.getRoute(routeId);
    }
}
