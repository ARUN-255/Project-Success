package com.bronzera.mybus.route;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "route_stop")
public class RouteStop{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "morning_time", nullable = false)
    private LocalTime morningTime;

    @Column(name = "evening_time", nullable = false)
    private LocalTime eveningTime;

    public Long getId(){
        return id;
    }

    public Route getRoute(){
        return route;
    }

    public void setRoute(Route route){
        this.route = route;
    }

    public Stop getStop(){
        return stop;
    }

    public void setStop(Stop stop){
        this.stop = stop;
    }

    public int getSequenceOrder(){
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder){
        this.sequenceOrder = sequenceOrder;
    }

    public LocalTime getMorningTime(){
        return morningTime;
    }

    public void setMorningTime(LocalTime morningTime){
        this.morningTime = morningTime;
    }

    public LocalTime getEveningTime(){
        return eveningTime;
    }

    public void setEveningTime(LocalTime eveningTime){
        this.eveningTime = eveningTime;
    }
}