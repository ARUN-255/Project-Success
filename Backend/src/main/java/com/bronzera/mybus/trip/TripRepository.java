package com.bronzera.mybus.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByRouteIdAndStatus(Long routeId, Trip.TripStatus status);

    Optional<Trip> findByDriverIdAndStatus(Long driverId, Trip.TripStatus status);

    List<Trip> findByStatus(Trip.TripStatus status);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r JOIN FETCH r.bus WHERE t.status = :status")
    List<Trip> findByStatusWithRouteAndBus(@Param("status") Trip.TripStatus status);

    boolean existsByRouteIdAndTripTypeAndStartedAtBetween(
        Long routeId,
        Trip.TripType tripType,
        OffsetDateTime start,
        OffsetDateTime end
    );
}