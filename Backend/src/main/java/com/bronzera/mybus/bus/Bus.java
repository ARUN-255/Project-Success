package com.bronzera.mybus.bus;

import com.bronzera.mybus.college.College;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bus")
public class Bus{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "bus_number", nullable = false, length = 30)
    private String busNumber;

    @Column(name = "registration_number", nullable = false, length = 30)
    private String registrationNumber;

    @Column(name = "bus_type", nullable = false, length = 20)
    private String busType;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    }

    public College getCollege(){
        return college;
    }

    public void setCollege(College college){
        this.college = college;
    }

    public String getBusNumber(){
        return busNumber;
    }

    public void setBusNumber(String busNumber){
        this.busNumber = busNumber;
    }

    public String getRegistrationNumber(){
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber){
        this.registrationNumber = registrationNumber;
    }

    public String getBusType(){
        return busType;
    }

    public void setBusType(String busType){
        this.busType = busType;
    }

    public String getPhotoUrl(){
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl){
        this.photoUrl = photoUrl;
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