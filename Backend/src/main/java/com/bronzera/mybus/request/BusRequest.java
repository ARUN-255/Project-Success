package com.bronzera.mybus.request;

import com.bronzera.mybus.route.Route;
import com.bronzera.mybus.student.Student;
import com.bronzera.mybus.trip.Trip;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity 
@Table(
    name = "bus_request",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "route_id", "request_date", "trip_type"})
)
public class BusRequest{

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 10)
    private Trip.TripType tripType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    }

    public Student getStudent(){
        return student;
    }

    public void setStudent(Student student){
        this.student = student;
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

    public LocalDate getRequestDate(){
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate){
        this.requestDate = requestDate;
    }

    public Trip.TripType getTripType() {
        return tripType;
    }

    public void setTripType(Trip.TripType tripType) {
        this.tripType = tripType;
    }   
    
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}