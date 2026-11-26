package com.bronzera.mybus.trip;

import com.bronzera.mybus.eta.EtaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final EtaService etaService;

    public TripController(TripService tripService , EtaService etaService){
        this.tripService = tripService;
        this.etaService = etaService;
    }

    @PostMapping("/start")
    public ResponseEntity<TripService.TripResponse> startTrip(@Valid @RequestBody StartTripRequest request){
        // TODO: take driverId from JWT token once JWT filter is added
        TripService.TripResponse response = tripService.startTrip(
            request.routeId(),
            request.driverId(),
            request.deviceId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/end")
    public TripService.TripResponse endTrip(@PathVariable Long id, @Valid @RequestBody EndTripRequest request){
        // TODO: take actorId and role from JWT token once JWT filter is added
        return tripService.endTrip(id, request.actorId(), request.admin());
    }

    @GetMapping("/active")
    public ResponseEntity<TripService.TripResponse> getActiveTrip(@RequestParam Long routeId){
        TripService.TripResponse active = tripService.getActiveTrip(routeId);
        return active != null ? ResponseEntity.ok(active) : ResponseEntity.noContent().build();
    }

    @GetMapping("/eta")
    public EtaService.EtaResponse getEta(@RequestParam Long routeId, @RequestParam Long stopId){
        Trip.TripType tripType = tripService.detectCurrentOrNextTripType(routeId);
        return etaService.estimateEta(routeId, stopId, tripType);
    }

    public record StartTripRequest(
        @NotNull Long routeId,
        @NotNull Long driverId,
        String deviceId
    ) {}

    public record EndTripRequest(
        @NotNull Long actorId,
        boolean admin
    ) {}
}