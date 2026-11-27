package com.bronzera.mybus.request;

import com.bronzera.mybus.trip.Trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();

boolean existsByRouteIdAndAlertTypeAndAlertDateAndTripType(
    Long routeId,
    Alert.AlertType alertType,
    LocalDate alertDate,
    Trip.TripType tripType  
);
}