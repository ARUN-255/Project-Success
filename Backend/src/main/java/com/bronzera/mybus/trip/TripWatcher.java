package com.bronzera.mybus.trip;

import com.bronzera.mybus.schedule.RunSchedule;
import com.bronzera.mybus.schedule.ScheduleRepository;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.time.LocalDateTime;

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

        checkAutoEnd(today, now);
        checkMissedStarts(today, now);
    }

    private void checkAutoEnd(LocalDate today, LocalTime now){
        OffsetDateTime todayStart = today.atStartOfDay(clock.getZone()).toOffsetDateTime();
        LocalDateTime nowDateTime = LocalDateTime.of(today, now);

        List<Trip> runningTrips = tripRepository.findByStatusWithRouteAndBus(Trip.TripStatus.RUNNING);
        for(Trip trip : runningTrips){
            try{
                if(trip.getStartedAt() != null && trip.getStartedAt().isBefore(todayStart)){
                    tripService.autoEndTrip(trip.getId());
                    continue;
                }

                String field = trip.getTripType() == Trip.TripType.EVENING ? "eveningTime" : "morningTime";
                LocalTime lastStopTime = entityManager.createQuery(
                    "SELECT MAX(rs. " + field + ") FROM RouteStop rs WHERE rs.route.id = :routeId", LocalTime.class)
                    .setParameter("routeId", trip.getRoute().getId()).getSingleResult();

                    if (lastStopTime != null){
                    LocalDate tripDate = trip.getStartedAt() != null
                        ? trip.getStartedAt().atZoneSameInstant(clock.getZone()).toLocalDate()
                        : today;
                    LocalDateTime autoEndAt = LocalDateTime.of(tripDate, lastStopTime).plusMinutes(autoEndMinutes);
                    if (nowDateTime.isAfter(autoEndAt)){
                        tripService.autoEndTrip(trip.getId());
                    }
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
                if(s.getMorningStart() != null && s.getMorningEnd() != null && now.isAfter(s.getMorningStart().plusMinutes(5)) && !now.isAfter(s.getMorningEnd())){
                    boolean started = tripRepository.existsByRouteIdAndTripTypeAndStartedAtBetween(routeId, Trip.TripType.MORNING, dayStart, dayEnd);
                    
                    if(!started) {
                        tripService.recordMissedStart(routeId, today, Trip.TripType.MORNING);
                    }
                }
                if(s.getEveningStart() != null && s.getEveningEnd() != null && now.isAfter(s.getEveningStart().plusMinutes(5)) && !now.isAfter(s.getEveningEnd())){
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
