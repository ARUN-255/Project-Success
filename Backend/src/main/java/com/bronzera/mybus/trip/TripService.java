package com.bronzera.mybus.trip;

import com.bronzera.mybus.driver.Driver;
import com.bronzera.mybus.driver.DriverRepository;
import com.bronzera.mybus.route.Route;
import com.bronzera.mybus.route.RouteRepository;
import com.bronzera.mybus.schedule.RunSchedule;
import com.bronzera.mybus.schedule.ScheduleRepository;
import com.bronzera.mybus.schedule.ScheduleService;
import com.bronzera.mybus.request.Alert;
import com.bronzera.mybus.request.AlertRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final AlertRepository alertRepository;

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public TripService(
        TripRepository tripRepository,
        RouteRepository routeRepository,
        DriverRepository driverRepository,
        ScheduleRepository scheduleRepository,
        ScheduleService scheduleService,
        StringRedisTemplate redisTemplate,
        AlertRepository alertRepository,
        Clock clock
    ){
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
        this.redisTemplate = redisTemplate;
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    @Transactional
    public TripResponse startTrip(Long routeId, Long driverId, String deviceId){
        Driver driver = driverRepository.findById(driverId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        if(!driver.isActive()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver account is disabled");
        }

        if(driver.getActiveDeviceId() != null && !driver.getActiveDeviceId().equals(deviceId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver is active on another device");
            }

         Optional<Trip> existing = tripRepository.findByRouteIdAndStatus(routeId, Trip.TripStatus.RUNNING);
            if(existing.isPresent() && existing.get().getDriver() != null && existing.get().getDriver().getId().equals(driverId)){
                return TripResponse.from(existing.get());
        }

        Route route = routeRepository.findById(routeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        if(!route.getBus().getCollege().getId().equals(driver.getCollege().getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver does not belong to this college");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        RunSchedule schedule = scheduleRepository.findByRouteIdAndRunDate(routeId, today).filter(RunSchedule::isRunning).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Route is not scheduled to run today"));

        Trip.TripType tripType = detectTripType(schedule, now);
        if(tripType == null || !scheduleService.isInsideActiveWindow(routeId, today, now, tripType.name())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip cannot start outside the scheduled run window");
        }

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setDriver(driver);
        trip.setTripType(tripType);
        trip.setStatus(Trip.TripStatus.RUNNING);
        trip.setStartedAt(OffsetDateTime.now(clock));

        try{
            return TripResponse.from(tripRepository.saveAndFlush(trip));
        }catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip already running");
        }
    }

    @Transactional
    public TripResponse endTrip(Long tripId, Long actorId, boolean isAdmin){
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        if(trip.getStatus() != Trip.TripStatus.RUNNING){
            return TripResponse.from(trip);
        }

        boolean isOwnDriver = trip.getDriver() != null && trip.getDriver().getId().equals(actorId);

        if(!isAdmin && !isOwnDriver){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the trip driver or an admin can end this trip");
        }

        trip.setStatus(Trip.TripStatus.ENDED);
        trip.setEndedAt(OffsetDateTime.now(clock));
        trip.setEndedBy(isAdmin ? Trip.EndedBy.ADMIN : Trip.EndedBy.DRIVER);
        Trip saved = tripRepository.save(trip);
        deleteRedisLive(tripId);
        return TripResponse.from(saved);
    }

    @Transactional
    public void autoEndTrip(Long tripId){
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if(trip == null || trip.getStatus() != Trip.TripStatus.RUNNING){
            return;
        }
        trip.setStatus(Trip.TripStatus.AUTO_ENDED);
        trip.setEndedAt(OffsetDateTime.now(clock));
        trip.setEndedBy(Trip.EndedBy.AUTO);
        tripRepository.save(trip);
        deleteRedisLive(tripId);

        LocalDate today = LocalDate.now(clock);
        if(alertRepository.existsByRouteIdAndAlertTypeAndAlertDateAndTripType(
            trip.getRoute().getId(), Alert.AlertType.AUTO_ENDED, today, trip.getTripType()
        )){
            return;
        }

        Alert alert = new Alert();
        alert.setAlertType(Alert.AlertType.AUTO_ENDED);
        alert.setRoute(trip.getRoute());
        alert.setTrip(trip);
        alert.setAlertDate(LocalDate.now(clock));
        alert.setTripType(trip.getTripType());
        alert.setAlertDate(today);
        try {
            alertRepository.saveAndFlush(alert);
        } catch (DataIntegrityViolationException e){
            log.debug("Alert already recorded: {}", e.getMessage());
        }
    }

    @Transactional
    public void recordMissedStart(Long routeId, LocalDate alertDate, Trip.TripType tripType){
        
        if(alertRepository.existsByRouteIdAndAlertTypeAndAlertDateAndTripType(
            routeId, Alert.AlertType.MISSED_START, alertDate, tripType
        )){
            return;
        }
        
        Route route = routeRepository.findById(routeId).orElse(null);
        if(route == null){
            return;
        }
        Alert alert = new Alert();
        alert.setAlertType(Alert.AlertType.MISSED_START);
        alert.setRoute(route);
        alert.setAlertDate(alertDate);
        alert.setTripType(tripType);
        try{
            alertRepository.saveAndFlush(alert);
        }catch (DataIntegrityViolationException e){
            log.debug("Missed-start alert already recorded: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TripResponse getActiveTrip(Long routeId){
        return tripRepository.findByRouteIdAndStatus(routeId, Trip.TripStatus.RUNNING).map(TripResponse::from).orElse(null);
    }

    private Trip.TripType detectTripType(RunSchedule s, LocalTime now){
        if (s.getMorningStart() != null && s.getMorningEnd() != null && !now.isBefore(s.getMorningStart()) && !now.isAfter(s.getMorningEnd())){
            return Trip.TripType.MORNING;
        }
        if(s.getEveningStart() != null && s.getEveningEnd() != null && !now.isBefore(s.getEveningStart()) && !now.isAfter(s.getEveningEnd())){
            return Trip.TripType.EVENING;
        }
        return null;
    }

    public Trip.TripType detectCurrentOrNextTripType(Long routeId) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        RunSchedule s = scheduleRepository.findByRouteIdAndRunDate(routeId, today)
        .filter(RunSchedule::isRunning).orElse(null);
        if(s == null ){
            return null;
        }
            Trip.TripType open = detectTripType(s, now);
            if(open != null) return open;
            if(s.getMorningEnd() != null && !now.isAfter(s.getMorningEnd())) return Trip.TripType.MORNING;
            if(s.getEveningEnd() != null) return Trip.TripType.EVENING;
    
        return null;
    }

    private void deleteRedisLive(Long tripId){
        try{
            redisTemplate.delete("trip:" + tripId + ":live");
        } catch (Exception e){
            log.warn("Could not delete Redis live key for trip " + tripId + ": " + e.getMessage());
        }
    }

    public record TripResponse(
        Long id,
        Long routeId,
        Long driverId,
        Trip.TripType tripType,
        Trip.TripStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Trip.EndedBy endedBy
    ){
        public static TripResponse from(Trip t){
            return new TripResponse(
                t.getId(),
                t.getRoute().getId(),
                t.getDriver() != null ? t.getDriver().getId() : null,
                t.getTripType(),
                t.getStatus(),
                t.getStartedAt(),
                t.getEndedAt(),
                t.getEndedBy()
            );
        }
    }
}