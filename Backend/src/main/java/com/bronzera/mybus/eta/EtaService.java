package com.bronzera.mybus.eta;

import com.bronzera.mybus.location.LocationService;
import com.bronzera.mybus.route.RouteService;
import com.bronzera.mybus.route.RouteStop;
import com.bronzera.mybus.trip.Trip;
import com.bronzera.mybus.trip.TripRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EtaService{

    public enum EtaBasis { LIVE, LEARNED, SCHEDULED, NOT_STARTED, PASSED}

    private final TripRepository tripRepository;
    private final RouteService routeService;
    private final LocationService locationService;
    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;
    private final Clock clock;

    public EtaService(
        TripRepository tripRepository,
        RouteService routeService,
        LocationService locationService,
        StringRedisTemplate redisTemplate,
        EntityManager entityManager,
        Clock clock
    ){
        this.tripRepository = tripRepository;
        this.routeService = routeService;
        this.locationService = locationService;
        this.redisTemplate = redisTemplate;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EtaResponse estimateEta(Long routeId, Long stopId, Trip.TripType tripType){
        boolean evening = tripType == Trip.TripType.EVENING;
        List<RouteStop> stops = routeService.getRouteStops(routeId, evening);
        RouteStop target = stops.stream()
        .filter(rs -> rs.getStop().getId().equals(stopId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not on route"));

        LocalTime now = LocalTime.now(clock);
        LocalTime scheduledTime = evening ? target.getEveningTime() : target.getMorningTime();
        Trip runningTrip = tripRepository.findByRouteIdAndStatus(routeId, Trip.TripStatus.RUNNING).orElse(null);

        if(runningTrip != null){
            if(hasPassedStop(runningTrip.getId(), stops, target)){
                return new EtaResponse(0, null, EtaBasis.PASSED);
            }

            LocationService.LiveLocationResponse live = locationService.getLiveLocation(runningTrip.getId());
            if(live != null && !live.stale()){
                double roadMeters = remainingRoadMeters(runningTrip.getId(), stops, target, live.lat(), live.lon());
                double speedKmh = Math.max(20.0, live.speedKmh());
                long etaSeconds = Math.max(0, Math.round((roadMeters / 1000.0) / speedKmh * 3600.0));
                return new EtaResponse(etaSeconds, now.plusSeconds(etaSeconds), EtaBasis.LIVE);
            }
        }
        Long learnedDelaySeconds = getLearnedDelaySeconds(routeId, stopId, tripType, scheduledTime);
        if(learnedDelaySeconds != null){
            LocalTime predicted = scheduledTime.plusSeconds(learnedDelaySeconds);
            if(now.isAfter(predicted)){
                return new EtaResponse(0, predicted, EtaBasis.NOT_STARTED);
            }
            long etaSeconds = ChronoUnit.SECONDS.between(now, predicted);
            return new EtaResponse(etaSeconds, predicted, EtaBasis.LEARNED);
        }

        if (now.isAfter(scheduledTime)){
            return new EtaResponse(0, scheduledTime, EtaBasis.NOT_STARTED);
        }

        long scheduledEta = ChronoUnit.SECONDS.between(now, scheduledTime);
        return new EtaResponse(scheduledEta, scheduledTime, EtaBasis.SCHEDULED);
    }

    private boolean hasPassedStop(Long tripId, List<RouteStop> stops, RouteStop target){
        @SuppressWarnings("unchecked")
        List<Number> arrivedStopIds = entityManager.createNativeQuery("SELECT stop_id FROM stop_arrival where trip_id = :tid").setParameter("tid", tripId).getResultList();

        if(arrivedStopIds.isEmpty()){
            return false;
        }
        int targetIndex = stops.indexOf(target);
        for(Number id : arrivedStopIds){
            long sid = id.longValue();
            if(sid == target.getStop().getId()){
                return true;
            }
            for(int i = targetIndex +1; i < stops.size(); i++){
                if(stops.get(i).getStop().getId().equals(sid)){
                    return true;
                }
            }
        }
        return false;
    }

    private double remainingRoadMeters(Long tripId, List<RouteStop> stops, RouteStop target, double lat, double lon){
        @SuppressWarnings("unchecked")
        List<Number> arrived = entityManager.createNativeQuery(
            "SELECT stop_id FROM stop_arrival WHERE trip_id = :tid"
        )
        .setParameter("tid", tripId)
        .getResultList();       
        java.util.Set<Long> arrivedIds = new java.util.HashSet<>();
        for(Number n : arrived) arrivedIds.add(n.longValue());

        int nextIdx = 0;
        while (nextIdx < stops.size() && arrivedIds.contains(stops.get(nextIdx).getStop().getId())){
            nextIdx++;
        }
        int targetIdx = stops.indexOf(target);
        if(nextIdx > targetIdx){
            return 0.0;
        }

        double total = LocationService.distanceMeters(lat, lon, stops.get(nextIdx).getStop().getLatitude(),stops.get(nextIdx).getStop().getLongitude());
        for(int i = nextIdx; i < targetIdx; i++){
            RouteStop a = stops.get(i);
            RouteStop b = stops.get(i + 1);
            total += LocationService.distanceMeters(a.getStop().getLatitude(), a.getStop().getLongitude(), b.getStop().getLatitude(), b.getStop().getLongitude());
        }
        return total * 1.3;
    }

    private Long getLearnedDelaySeconds(Long routeId, Long stopId, Trip.TripType tripType, LocalTime scheduledTime) {
        boolean isSaturday = LocalDate.now(clock).getDayOfWeek() == DayOfWeek.SATURDAY;
        String dayGroup = isSaturday ? "SAT" : "WKDAY";
        String cacheKey = "eta:delay:" + routeId + ":" + stopId + ":" + tripType.name() + ":" + dayGroup;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if(cached  != null){
            return "NONE".equals(cached) ? null : Long.parseLong(cached);
        }

        String timeCol = tripType == Trip.TripType.EVENING ? "rs.evening_time" : "rs.morning_time";
        String dowFilter = isSaturday ? "EXTRACT(DOW FROM(sa.arrived_at AT TIME ZONE 'Asia/Kolkata')) = 6" : "EXTRACT(DOW FROM (sa.arrived_at AT TIME ZONE 'Asia/Kolkata')) BETWEEN 1 AND 5";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
            "SELECT COUNT(*), AVG(EXTRACT(EPOCH FROM ((sa.arrived_at AT TIME ZONE 'Asia/Kolkata')::time -" + timeCol + "))) "
            + "FROM stop_arrival sa "
            + "JOIN trip t ON t.id = sa.trip_id "
            + "JOIN route_stop rs ON rs.route_id = t.route_id AND rs.stop_id = sa.stop_id "
            + "WHERE t.route_id = :rid AND sa.stop_id = :sid AND t.trip_type = :tt "
            + "AND sa.arrived_at >= NOW() - INTERVAL '30 days' "
            + "AND " + dowFilter + " "
            + "AND ABS(EXTRACT(EPOCH FROM ((sa.arrived_at AT TIME ZONE 'Asia/Kolkata')::time - " + timeCol + "))) <= 3600")

            .setParameter("rid", routeId)
            .setParameter("sid", stopId)
            .setParameter("tt", tripType.name())
            .getResultList();

            if(!rows.isEmpty()){
                long count = ((Number) rows.get(0)[0]).longValue();
                if(count >= 5 && rows.get(0)[1] != null){
                    long avgDelay = Math.round(((Number) rows.get(0)[1]).doubleValue());
                    redisTemplate.opsForValue().set(cacheKey, String.valueOf(avgDelay), Duration.ofMinutes(10));
                    return avgDelay;
                }
            }
            redisTemplate.opsForValue().set(cacheKey, "NONE", Duration.ofMinutes(10));
            return null;
    } 
    public record EtaResponse(long etaSeconds, LocalTime arrivalTime, EtaBasis basis) {}
}