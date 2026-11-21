package com.bronzera.mybus.route;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteResponse> listRoutes(@RequestParam Long collegeId) {
        return routeService.listRoutesByCollege(collegeId).stream()
                .map(RouteResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@RequestBody CreateRouteRequest request) {
        Route route = routeService.createRoute(request.busId(), request.name(), request.stops());
        return ResponseEntity.status(HttpStatus.CREATED).body(RouteResponse.from(route));
    }

    @GetMapping("/{routeId}/stops")
    public List<RouteStopResponse> getRouteStops(
            @PathVariable Long routeId,
            @RequestParam(defaultValue = "false") boolean evening) {
        return routeService.getRouteStops(routeId, evening).stream()
                .map(RouteStopResponse::from)
                .toList();
    }

    @GetMapping("/stops")
    public List<StopResponse> listStops(@RequestParam Long collegeId) {
        return routeService.listStopsByCollege(collegeId).stream()
                .map(StopResponse::from)
                .toList();
    }

    @PostMapping("/stops")
    public ResponseEntity<StopResponse> createStop(@RequestBody CreateStopRequest request) {
        Stop stop = routeService.createStop(
                request.collegeId(),
                request.name(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(StopResponse.from(stop));
    }

    public record CreateStopRequest(Long collegeId, String name, double latitude, double longitude) {}

    public record CreateRouteRequest(Long busId, String name, List<RouteService.StopTimeInput> stops) {}

    public record RouteResponse(Long id, Long busId, String name, boolean active) {
        public static RouteResponse from(Route route) {
            return new RouteResponse(route.getId(), route.getBus().getId(), route.getName(), route.isActive());
        }
    }

    public record StopResponse(Long id, String name, double latitude, double longitude) {
        public static StopResponse from(Stop stop) {
            return new StopResponse(stop.getId(), stop.getName(), stop.getLatitude(), stop.getLongitude());
        }
    }

    public record RouteStopResponse(
            Long stopId,
            String stopName,
            double latitude,
            double longitude,
            int sequenceOrder,
            LocalTime morningTime,
            LocalTime eveningTime
    ) {
        public static RouteStopResponse from(RouteStop rs) {
            Stop s = rs.getStop();
            return new RouteStopResponse(
                    s.getId(),
                    s.getName(),
                    s.getLatitude(),
                    s.getLongitude(),
                    rs.getSequenceOrder(),
                    rs.getMorningTime(),
                    rs.getEveningTime()
            );
        }
    }
}