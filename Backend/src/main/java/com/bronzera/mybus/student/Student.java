package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "student")
public class Student {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "reg_no", nullable = false, length = 60)
    private String regNo;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(name = "college_email", unique = true, length = 255)
    private String collegeEmail;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_dob_attempts", nullable = false)
    private int failedDobAttempts = 0;

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

    public String getRegNo(){
        return regNo;
    }

    public void setRegNo(String regNo){
        this.regNo = regNo;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getAddress(){
        return address;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public String getDepartment(){
        return department;
    }

    public void setDepartment(String department){
        this.department = department;
    }

    public int getYear(){
        return year;
    }

    public void setYear(int year){
        this.year = year;
    }

    public LocalDate getDob(){
        return dob;
    }

    public void setDob(LocalDate dob){
        this.dob = dob;
    }

    public String getCollegeEmail(){
        return collegeEmail;
    }

    public void setCollegeEmail(String collegeEmail){
        this.collegeEmail = collegeEmail;
    }

    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public int getFailedDobAttempts(){
        return failedDobAttempts;
    }

    public void setFailedDobAttempts(int failedDobAttempts){
        this.failedDobAttempts = failedDobAttempts;
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
