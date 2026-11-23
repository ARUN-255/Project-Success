package com.bronzera.mybus.driver;

import com.bronzera.mybus.college.College;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "driver")
public class Driver {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "login_id", nullable = false, unique = true, length = 80)
    private String loginId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "active_device_id", length = 255)
    private String activeDeviceId;

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

    public String getLoginId(){
        return loginId;
    }

    public void setLoginId(String loginId){
        this.loginId = loginId;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public LocalDate getDob(){
        return dob;
    }

    public void setDob(LocalDate dob){
        this.dob = dob;
    }

    public boolean isMustChangePassword(){
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword){
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public int getFailedAttempts(){
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts){
        this.failedAttempts = failedAttempts;
    }

    public OffsetDateTime getLockedUntil(){
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil){
        this.lockedUntil = lockedUntil;
    }

    public String getActiveDeviceId(){
        return activeDeviceId;
    }

    public void setActiveDeviceId(String activeDeviceId){
        this.activeDeviceId = activeDeviceId;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}