package com.bronzera.mybus.bus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByCollegeIdAndActiveTrue(Long collegeId);
    List<Bus> findByCollegeId(Long collegeId);
    boolean existsByCollegeIdAndBusNumberIgnoreCase(Long collegeId, String busNumber);
}