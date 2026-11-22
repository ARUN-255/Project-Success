package com.bronzera.mybus.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<RunSchedule, Long>{
    Optional<RunSchedule> findByRouteIdAndRunDate(Long routeId, LocalDate runDate);
    List<RunSchedule> findByRouteIdAndRunDateBetweenOrderByRunDateAsc(Long routeId, LocalDate start, LocalDate end);
    List<RunSchedule> findByRunDateAndRunningTrue(LocalDate runDate);
}