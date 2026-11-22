package com.bronzera.mybus.admin;

import com.bronzera.mybus.college.College;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_owner", nullable = false)
    private boolean owner = false;

    @Column(name = "owner_access_requested", nullable = false)
    private boolean ownerAccessRequested = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

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

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public boolean isOwner(){
        return owner;
    }

    public void setOwner(boolean owner){
        this.owner = owner;
    }

    public boolean isOwnerAccessRequested(){
        return ownerAccessRequested;
    }

    public void setOwnerAccessRequested(boolean ownerAccessRequested){
        this.ownerAccessRequested = ownerAccessRequested;
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

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}