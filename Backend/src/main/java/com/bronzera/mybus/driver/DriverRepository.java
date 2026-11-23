package com.bronzera.mybus.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long>{
    Optional<Driver> findByLoginIdIgnoreCase(String loginId);
    boolean existsByLoginIdIgnoreCase(String loginId);
    List<Driver> findByCollegeId(Long collegeId);
}