package com.bronzera.mybus.eta;

import com.bronzera.mybus.route.Stop;
import com.bronzera.mybus.trip.Trip;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stop_arrival")
public class StopArrival{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "arrived_at", nullable = false)
    private OffsetDateTime arrivedAt;

    public Long getId(){
        return id;
    }

    public Trip getTrip(){
        return trip;
    }

    public void setTrip(Trip trip){
        this.trip = trip;
    }

    public Stop getStop(){
        return stop;
    }

    public void setStop(Stop stop){
        this.stop = stop;
    }

    public OffsetDateTime getArrivedAt(){
        return arrivedAt;
    }

    public void setArrivedAt(OffsetDateTime arrivedAt){
        this.arrivedAt = arrivedAt;
    }
}