package com.bronzera.mybus.trip;

import com.bronzera.mybus.driver.Driver;
import com.bronzera.mybus.route.Route;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trip")
public class Trip{

    public enum TripType {MORNING, EVENING}
    public enum TripStatus {RUNNING, ENDED, AUTO_ENDED}
    public enum EndedBy {DRIVER, AUTO, ADMIN}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 10)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ended_by", length = 20)
    private EndedBy endedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    } 

    public Route getRoute(){
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Driver getDriver(){
        return driver;
    }

    public void setDriver(Driver driver){
        this.driver = driver;
    }

    public TripType getTripType(){
        return tripType;
    }

    public void setTripType(TripType tripType) {
        this.tripType = tripType;
    }

    public TripStatus getStatus(){
        return status;
    }

    public void setStatus(TripStatus status){
        this.status = status;
    }

    public OffsetDateTime getStartedAt(){
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt){
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt(){
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt){
        this.endedAt = endedAt;
    }

    public EndedBy getEndedBy(){
        return endedBy;
    }

    public void setEndedBy(EndedBy endedBy){
        this.endedBy = endedBy;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}