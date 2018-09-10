package ru.bpc.cm.routes.boot.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.GeocodingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRoutePointItem;
import ru.bpc.cm.items.routes.DepotItem;
import ru.bpc.cm.routes.boot.dao.RoutePointDao;
import java.util.ArrayList;

import static ru.bpc.cm.routes.boot.RoutesBootConstants.GOOGLE_API_KEY;

@Service("routePointService")
public class RoutePointServiceImpl implements RoutePointService {

    private Logger slf4jLog = LoggerFactory.getLogger(RoutePointServiceImpl.class);

    private final RoutePointDao routePointDao;

    @Autowired
    public RoutePointServiceImpl(RoutePointDao routePointDao) {
        this.routePointDao = routePointDao;
    }

    @Override
    public ArrayList<AtmRoutePointItem> getPoints(int routeN) {
        return routePointDao.getPoints(routeN);
    }

    @Override
    public ArrayList<AtmRoutePointItem> getPoints(AtmRouteFilter filter) {
        return routePointDao.getPoints(filter);
    }

    @Override
    public ArrayList<AtmRoutePointItem> geocodeAtmPoints(ArrayList<AtmRoutePointItem> points) throws Exception {
        GeoApiContext context = new GeoApiContext().setApiKey(GOOGLE_API_KEY);
        String address = null;
        GeocodingApiRequest request = GeocodingApi.newRequest(context);
        for (AtmRoutePointItem atm : points) {
            address = atm.getAdress();
            GeocodingResult[] result = request.address(address)
                    .language("en")
                    .await();
            if (result.length == 0) {
                atm.setLatitude("0");
                atm.setLongitude("0");
            } else {
                if (result[0].partialMatch) {
                    atm.setLatitude("0");
                    atm.setLongitude("0");
                } else {
                    atm.setLatitude(Double.toString(result[0].geometry.location.lat));
                    atm.setLongitude(Double.toString(result[0].geometry.location.lng));
                    String[] addressArray = atm.getAdress().split(", "); // TODO change after DB modification
                    routePointDao.updateAtmPointGeo(addressArray[0], addressArray[1],
                            atm.getLatitude(), atm.getLongitude()); //TODO change after DB modification
                }
            }
        }
        return points;
    }

    @Override
    public ArrayList<DepotItem> geocodeDepotPoints(ArrayList<DepotItem> points) throws Exception {
        GeoApiContext context = new GeoApiContext().setApiKey(GOOGLE_API_KEY);
        String address;
        GeocodingApiRequest request = GeocodingApi.newRequest(context);
        for (DepotItem depot : points) {
            address = depot.getStreet() + ", " + depot.getCity() + ", " + depot.getState();
            GeocodingResult[] result = request.address(address)
                    .language("en")
                    .await();
            if (result.length == 0) {
                depot.setLatitude("0");
                depot.setLongitude("0");
            } else {
                if (result[0].partialMatch) {
                    depot.setLatitude("0");
                    depot.setLongitude("0");
                } else {
                    depot.setLatitude(Double.toString(result[0].geometry.location.lat));
                    depot.setLongitude(Double.toString(result[0].geometry.location.lng));
                    routePointDao.updateDepotPointGeo(depot.getCity(), depot.getStreet(),
                            depot.getLatitude(), depot.getLongitude());
                }
            }
        }
        return points;
    }
}
