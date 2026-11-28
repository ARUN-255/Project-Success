package com.bronzera.mybus.college;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
    
    boolean existsByNameIgnoreCase(String name);
}