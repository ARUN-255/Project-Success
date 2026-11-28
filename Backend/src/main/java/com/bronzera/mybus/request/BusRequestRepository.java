package com.bronzera.mybus.request;

import com.bronzera.mybus.trip.Trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BusRequestRepository extends JpaRepository<BusRequest, Long> {
    boolean existsByStudentIdAndRouteIdAndRequestDateAndTripType(
        Long studentId,
        Long routeId,
        LocalDate requestDate,
        Trip.TripType tripType 
    );

    @Query("SELECT br FROM BusRequest br JOIN FETCH br.student JOIN FETCH br.route "
         + "WHERE br.route.bus.college.id = :collegeId AND br.requestDate = :date "
         + "ORDER BY br.createdAt DESC")
    List<BusRequest> findByCollegeAndDate(@Param("collegeId") Long collegeId, @Param("date") LocalDate date);
}