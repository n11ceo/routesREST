package ru.bpc.cm.routes.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.bpc.cm.items.routes.AtmLocationItem;
import ru.bpc.cm.items.routes.AtmRouteFilter;
import ru.bpc.cm.items.routes.AtmRouteItem;
import ru.bpc.cm.items.routes.RoutingException;
import ru.bpc.cm.routes.boot.service.RouteService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;
import static ru.bpc.cm.routes.boot.RoutesBootConstants.*;

@RestController
@RequestMapping(path = "/routes")
public class RouteController {

    private Logger slf4jLog = LoggerFactory.getLogger(RouteController.class);

    private final
    RouteService routeService;

    @Autowired
    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<AtmRouteItem> getRoutes(
            @RequestParam(name = "dateStart") @DateTimeFormat(iso = ISO.DATE) Date dateStart,
            @RequestParam(name = "region", defaultValue = REGION) int region,
            @RequestParam(name = "maxPoints", defaultValue = MAX_POINTS) int maxPoints,
            @RequestParam(name = "maxCars", defaultValue = MAX_CARS) int maxCars,
            @RequestParam(name = "newDate", defaultValue = NEW_DATE) @DateTimeFormat(iso = ISO.DATE) Date newDate,
            @RequestParam(name = "optType", defaultValue = OPTIMIZATION_TYPE) int optType,
            @RequestParam(name = "optAlg", defaultValue = OPTIMIZATION_ALGO) int optAlg) {
        AtmRouteFilter filter = new AtmRouteFilter(dateStart, region, maxPoints, maxCars, newDate, optType, optAlg);
        return routeService.getRoutes(filter);
    }

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public void addRoute(@RequestBody @DateTimeFormat(iso = ISO.DATE) AtmRouteFilter filter) { //TODO good return
        routeService.addRoute(filter);
        slf4jLog.debug("Route successfully added");
    }

    @DeleteMapping(path = "/{id}",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@RequestBody @DateTimeFormat(iso = ISO.DATE) AtmRouteFilter filter,
                            @PathVariable int id) {
        System.out.println(id);
        System.out.println(filter.getDateStart());
        System.out.println(filter.getRegion());
        routeService.deleteRoute(id, filter);
        slf4jLog.debug("Route successfully deleted");
    }

    @PatchMapping(path = "/{id}/approve",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public AtmRouteItem approveRoute(@PathVariable("id") int id) {
        return routeService.approveRoute(id);
    }

    @PostMapping(path = "/calculate",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<AtmRouteItem> calculateRoutes(@RequestBody AtmRouteFilter filter) throws RoutingException {
        String uid = "1"; //uid is hardcoded, change it further(either on front and back)
        List<AtmLocationItem> atms = routeService.checkMatrixByRegionDate(filter.getRegion(),
                filter.getDateStart());
        if (atms.isEmpty()) {
            slf4jLog.debug("size: " + atms.size());
            slf4jLog.debug("matrix exists");
        } else {
            slf4jLog.debug("Calculating matrix");
            routeService.getPidsForMatrix(atms, filter.getRegion());
        }
        routeService.calculateOptimalRoute(uid, filter);
        return routeService.getRoutes(filter);
    }

    @PatchMapping(path = "/{id}/recalculate",
            consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public AtmRouteItem recalculateRoute(@RequestBody AtmRouteFilter filter, @PathVariable("id") int routeId)
            throws RoutingException {
        return routeService.recalculateRoute(filter, routeId);
    }

}
