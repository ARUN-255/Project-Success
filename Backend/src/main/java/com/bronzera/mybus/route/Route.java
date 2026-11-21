package com.bronzera.mybus.route;

import com.bronzera.mybus.bus.Bus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity 
@Table(name = "route")
public class Route {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    }

    public Bus getBus(){
        return bus;
    }

    public void setBus(Bus bus){
        this.bus = bus;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}