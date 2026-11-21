package com.bronzera.mybus.college;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "college")
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "email_domain", nullable = false, length = 120)
    private String emailDomain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getEmailDomain(){
        return emailDomain;
    }

    public void setEmailDomain(String emailDomain){
        this.emailDomain = emailDomain;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
}