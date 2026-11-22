package com.bronzera.mybus.schedule;

import com.bronzera.mybus.route.Route;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "run_schedule")
public class RunSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "morning_start")
    private LocalTime morningStart;

    @Column(name = "morning_end")
    private LocalTime morningEnd;

    @Column(name = "evening_start")
    private LocalTime eveningStart;

    @Column(name = "evening_end")
    private LocalTime eveningEnd;

    @Column(name = "is_running", nullable = false)
    private boolean running = true;

    public Long getId(){
        return id;
    }

    public Route getRoute(){
        return route;
    }

    public void setRoute(Route route){
        this.route = route;
    }

    public LocalDate getRunDate(){
        return runDate;
    }

    public void setRunDate(LocalDate runDate){
        this.runDate = runDate;
    }

    public LocalTime getMorningStart(){
        return morningStart;
    }

    public void setMorningStart(LocalTime morningStart){
        this.morningStart = morningStart;
    }

    public LocalTime getMorningEnd(){
        return morningEnd;
    }

    public void setMorningEnd(LocalTime morningEnd){
        this.morningEnd = morningEnd;
    }

    public LocalTime getEveningStart(){
        return eveningStart;
    }

    public void setEveningStart(LocalTime eveningStart){
        this.eveningStart = eveningStart;
    }

    public LocalTime getEveningEnd(){
        return eveningEnd;
    }

    public void setEveningEnd(LocalTime eveningEnd){
        this.eveningEnd = eveningEnd;
    }

    public boolean isRunning(){
        return running;
    }

    public void setRunning(boolean running){
        this.running = running;
    }
}