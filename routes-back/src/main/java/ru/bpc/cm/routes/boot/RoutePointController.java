package ru.bpc.cm.routes.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRoutePointItem;
import ru.bpc.cm.items.routes.DepotItem;
import ru.bpc.cm.routes.boot.service.RoutePointService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.bpc.cm.routes.boot.RoutesBootConstants.*;
import static ru.bpc.cm.routes.boot.RoutesBootConstants.OPTIMIZATION_ALGO;
import static ru.bpc.cm.routes.boot.RoutesBootConstants.OPTIMIZATION_TYPE;

@RestController
@RequestMapping("/routes")
public class RoutePointController {

    private final
    RoutePointService routePointService;

    @Autowired
    public RoutePointController(RoutePointService routePointService) {
        this.routePointService = routePointService;
    }

    @GetMapping("{id}/points")
    public List<AtmRoutePointItem> getRoutePoints(@PathVariable("id") int id) {
        return routePointService.getPoints(id);
    }

    @GetMapping(path = RoutesBootConstants.DEFAULT_POINT_PATH)
    public List<AtmRoutePointItem> getDefaultPoints(
            @RequestParam(name = "dateStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateStart,
            @RequestParam(name = "region", defaultValue = REGION) int region,
            @RequestParam(name = "maxPoints", defaultValue = MAX_POINTS) int maxPoints,
            @RequestParam(name = "maxCars", defaultValue = MAX_CARS) int maxCars,
            @RequestParam(name = "newDate", defaultValue = NEW_DATE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date newDate,
            @RequestParam(name = "optType", defaultValue = OPTIMIZATION_TYPE) int optType,
            @RequestParam(name = "optAlg", defaultValue = OPTIMIZATION_ALGO) int optAlg) {
        AtmRouteFilter filter = new AtmRouteFilter(dateStart, region, maxPoints, maxCars, newDate, optType, optAlg);
        return routePointService.getPoints(filter);
    }

    @PatchMapping(path = "/geocode/atms",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<AtmRoutePointItem> geocodeAtmPoints(@RequestBody ArrayList<AtmRoutePointItem> pointsAtm)
            throws Exception {
        return routePointService.geocodeAtmPoints(pointsAtm);
    }

    @PatchMapping(path = "/geocode/depot",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<DepotItem> geocodeDepotPoints(@RequestBody ArrayList<DepotItem> pointsDepot)
            throws Exception {
        return routePointService.geocodeDepotPoints(pointsDepot);
    }

}
