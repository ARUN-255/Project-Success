package com.bronzera.mybus.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController{

    private final LocationService locationService;

    public LocationController(LocationService locationService){
        this.locationService = locationService;
    }

    @PostMapping("/driver-ping")
    public ResponseEntity<Void> driverPing(@Valid @RequestBody DriverPingRequest request){
        // TODO : take driverId from JWT token once JWT filter is added
        locationService.recordDriverPing(
            request.tripId(),
            request.driverId(),
            request.latitude(),
            request.longitude(),
            request.speedMps(),
            request.accuracy(),
            request.clientTimeMs()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/student-ping")
    public ResponseEntity<Void> studentPing(@Valid @RequestBody StudentPingRequest request){
        // TODO : take studentId from JWT token once JWT filter is added
        locationService.recordStudentPing(
            request.routeId(),
            request.studentId(),
            request.latitude(),
            request.longitude(),
            request.speedMps(),
            request.accuracy(),
            request.clientTimeMs()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/live")
    public ResponseEntity<LocationService.LiveLocationResponse> getLive(
        @RequestParam(required = false) Long tripId,
        @RequestParam(required = false) Long routeId){
        LocationService.LiveLocationResponse live = (routeId != null)
        ? locationService.getLiveLocationByRoute(routeId)
        : locationService.getLiveLocation(tripId);
        return live != null ? ResponseEntity.ok(live) : ResponseEntity.noContent().build();
    }

    public record DriverPingRequest(
        @NotNull Long tripId,
        @NotNull Long driverId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Double speedMps,
        Double accuracy,
        Long clientTimeMs
    ) {}

    public record StudentPingRequest(
        @NotNull Long routeId,
        @NotNull Long studentId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Double speedMps,
        Double accuracy,
        Long clientTimeMs
    ) {}
}