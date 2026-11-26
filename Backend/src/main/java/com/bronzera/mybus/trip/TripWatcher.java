package com.bronzera.mybus.trip;

import com.bronzera.mybus.schedule.RunSchedule;
import com.bronzera.mybus.schedule.ScheduleRepository;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class TripWatcher {

    private static final Logger log = LoggerFactory.getLogger(TripWatcher.class);

    private final TripRepository tripRepository;
    private final TripService tripService;
    private final ScheduleRepository scheduleRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final long autoEndMinutes;

    public TripWatcher(
        TripRepository tripRepository,
        TripService tripService,
        ScheduleRepository scheduleRepository,
        EntityManager entityManager,
        Clock clock,
        @Value("${app.trip.auto-end-minutes:5}") long autoEndMinutes
    ){
        this.tripRepository = tripRepository;
        this.tripService = tripService;
        this.scheduleRepository = scheduleRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.autoEndMinutes = autoEndMinutes;
    }

    @Scheduled(fixedDelay = 30000)
    public void watchTrips(){
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        checkAutoEnd(now);
        checkMissedStarts(today, now);
    }

    private void checkAutoEnd(LocalTime now){
        List<Trip> runningTrips = tripRepository.findByStatusWithRouteAndBus(Trip.TripStatus.RUNNING);
        for(Trip trip : runningTrips){
            try{
                String field = trip.getTripType() == Trip.TripType.EVENING ? "eveningTime" : "morningTime";
                LocalTime lastStopTime = entityManager.createQuery(
                    "SELECT MAX(rs." + field + ") FROM RouteStop rs WHERE rs.route.id = :routeId",LocalTime.class).setParameter("routeId", trip.getRoute().getId()).getSingleResult();

                    if (lastStopTime != null && now.isAfter(lastStopTime.plusMinutes(autoEndMinutes))){
                        tripService.autoEndTrip(trip.getId());
                    }
            }catch (Exception e){
                log.warn("Auto-end check failed for trip {}: {}", trip.getId(), e.getMessage());
            }
        }
    }

    private void checkMissedStarts(LocalDate today, LocalTime now) {
        OffsetDateTime dayStart = today.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        List<RunSchedule> schedules = scheduleRepository.findByRunDateAndRunningTrue(today);
        for(RunSchedule s : schedules) {
            try{
                Long routeId = s.getRoute().getId();
                if(s.getMorningStart() != null && now.isAfter(s.getMorningStart().plusMinutes(5))){
                    boolean started = tripRepository.existsByRouteIdAndTripTypeAndStartedAtBetween(routeId, Trip.TripType.MORNING, dayStart, dayEnd);
                    
                    if(!started) {
                        tripService.recordMissedStart(routeId, today, Trip.TripType.MORNING);
                    }
                }
                if(s.getEveningStart() != null && now.isAfter(s.getEveningStart().plusMinutes(5))){
                    boolean started = tripRepository.existsByRouteIdAndTripTypeAndStartedAtBetween(routeId, Trip.TripType.EVENING, dayStart, dayEnd);
                    if(!started) {
                        tripService.recordMissedStart(routeId, today, Trip.TripType.EVENING);
                    }
                }
            } catch (Exception e) {
                log.warn("Missed-start check failed for schedule {}: {}", s.getId(), e.getMessage());
            }
        }
    }
}