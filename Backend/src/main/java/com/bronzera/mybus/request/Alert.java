package com.bronzera.mybus.request;

import com.bronzera.mybus.route.Route;
import com.bronzera.mybus.trip.Trip;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity 
@Table(name = "alert")
public class Alert{

    public enum AlertType { MISSED_START, AUTO_ENDED, SIGNAL_LOST, BUS_NOT_RUNNING}

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "request_count", nullable = false)
    private int requestCount = 0;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", length = 10)
    private Trip.TripType tripType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    }

    public AlertType getAlertType(){
        return alertType;
    }

    public void setAlertType(AlertType alertType){
        this.alertType = alertType;
    }

    public Route getRoute(){
        return route;
    }

    public void setRoute(Route route){
        this.route = route;
    }

    public Trip getTrip(){
        return trip;
    }

    public void setTrip(Trip trip){
        this.trip = trip;
    }

    public int getRequestCount(){
        return requestCount;
    }

    public void setRequestCount(int requestCount){
        this.requestCount = requestCount;
    }

    public boolean isResolved(){
        return resolved;
    }

    public void setResolved(boolean resolved){
        this.resolved = resolved;
    }

    public LocalDate getAlertDate(){
        return alertDate;
    }

    public void setAlertDate(LocalDate alertDate){
        this.alertDate = alertDate;
    }

    public Trip.TripType getTripType(){
        return tripType;
    }

    public void setTripType(Trip.TripType tripType){
        this.tripType = tripType;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}