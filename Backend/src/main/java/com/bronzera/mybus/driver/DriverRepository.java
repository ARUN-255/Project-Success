package com.bronzera.mybus.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long>{
    Optional<Driver> findByLoginIdIgnoreCase(String loginId);
    boolean existsByLoginIdIgnoreCase(String loginId);
    List<Driver> findByCollegeId(Long collegeId);
    Page<Driver> findByCollegeId(Long collegeId, Pageable pageable);
}