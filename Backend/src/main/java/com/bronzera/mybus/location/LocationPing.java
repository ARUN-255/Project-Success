package com.bronzera.mybus.location;

import com.bronzera.mybus.trip.Trip;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "location_ping")
public class LocationPing{
    public enum PingSource {DRIVER, STUDENT}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private Double speed;
    
    private Double accuracy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PingSource source;

    @Column(name = "source_user_id")
    private Long sourceUserId;

    public Long getId(){
        return id;
    }

    public Trip getTrip(){
        return trip;
    }

    public  void setTrip(Trip trip){
        this.trip = trip;
    }

    public OffsetDateTime getRecordedAt(){
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt){
        this.recordedAt = recordedAt;
    }

    public double getLatitude(){
        return latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public Double getSpeed(){
        return speed;
    }

    public void setSpeed(Double speed){
        this.speed = speed;
    }

    public Double getAccuracy(){
        return accuracy;
    }

    public void setAccuracy(Double accuracy){
        this.accuracy = accuracy;
    }

    public PingSource getSource(){
        return source;
    }

    public void setSource(PingSource source){
        this.source = source;
    }

    public Long getSourceUserId(){
        return sourceUserId;
    }

    public void setSourceUserId(Long sourceUserId){
        this.sourceUserId = sourceUserId;
    }
}