package com.bronzera.mybus.location;

import com.bronzera.mybus.route.RouteService;
import com.bronzera.mybus.route.RouteStop;
import com.bronzera.mybus.student.Student;
import com.bronzera.mybus.student.StudentRepository;
import com.bronzera.mybus.trip.Trip;
import com.bronzera.mybus.trip.TripRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationService{

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locationRepository;
    private final TripRepository tripRepository;
    private final StudentRepository studentRepository;
    private final RouteService routeService;
    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;
    private final Clock clock;

    public LocationService(
        LocationRepository locationRepository,
        TripRepository tripRepository,
        StudentRepository studentRepository,
        RouteService routeService,
        StringRedisTemplate redisTemplate,
        EntityManager entityManager,
        Clock clock
    ){
        this.locationRepository = locationRepository;
        this.tripRepository = tripRepository;
        this.studentRepository = studentRepository;
        this.routeService = routeService;
        this.redisTemplate = redisTemplate;
        this.entityManager = entityManager;
        this.clock = clock; 
    }

    @Transactional
    public void recordDriverPing(Long tripId, Long driverId, double lat, double lon, Double speedMps, Double accuracy, Long clientTimeMs){
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if(trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the trip driver");
        }
        if(trip.getStatus() != Trip.TripStatus.RUNNING){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip has already ended");
        }
        processPing(trip, LocationPing.PingSource.DRIVER, driverId, lat, lon, speedMps, accuracy, clientTimeMs);
    }

    @Transactional
    public void recordStudentPing(Long routeId, Long studentId, double lat, double lon, Double speedMps, Double accuracy, Long clientTimeMs){
        Trip trip = tripRepository.findByRouteIdAndStatus(routeId, Trip.TripStatus.RUNNING).orElse(null);

        if(trip == null){
            return;
        }
        Student student = studentRepository.findById(studentId).orElse(null);
        
        if(student == null || !student.isActive() || !student.isShareLocationAllowed()){
            return;
        }

        Number prefCount = (Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM student_route_pref WHERE student_id = :sid AND route_id = :rid AND trip_type = :tt")
            .setParameter("sid", studentId)
            .setParameter("rid", routeId)
            .setParameter("tt", trip.getTripType().name())
            .getSingleResult();

            if(prefCount.longValue() == 0) {
                return;
            }

            double speedKmh = toKmh(speedMps);
            if(speedKmh < 8.0){
                return;
            }

            boolean evening = trip.getTripType() == Trip.TripType.EVENING;
            List<RouteStop> stops = routeService.getRouteStops(routeId, evening);
            boolean nearRoute = stops.stream().anyMatch(rs -> distanceMeters(lat, lon, rs.getStop().getLatitude(), rs.getStop().getLongitude()) <= 2000.0);

            if(!nearRoute){
                return;
            }

            processPing(trip, LocationPing.PingSource.STUDENT, studentId, lat, lon, speedMps, accuracy, clientTimeMs);
    }

    private void processPing(
        Trip trip,
        LocationPing.PingSource source,
        Long userId,
        double lat,
        double lon,
        Double speedMps,
        Double accuracy,
        Long clientTimeMs
    ){
        if(lat < -90 || lat > 90 || lon < -180 || lon > 180 || (lat == 0.0 && lon == 0.0 )){
            return;
        }

        if(accuracy != null && accuracy > 100.0){
            return;
        }

        if(trip.getStatus() != Trip.TripStatus.RUNNING){
            return;
        }

        double speedKmh = toKmh(speedMps);
        long nowMs = clock.millis();
        String liveKey = "trip:" + trip.getId() + ":live";
        
        Boolean writeDb = Boolean.TRUE;
        try{
            Map<Object, Object> current = redisTemplate.opsForHash().entries(liveKey);
            if(!current.isEmpty()){
                double prevLat = Double.parseDouble((String) current.get("lat"));
                double prevLon = Double.parseDouble((String) current.get("lon"));
                long prevTs = Long.parseLong((String) current.get("ts"));

                if(clientTimeMs != null && clientTimeMs < nowMs - 15_000L){
                    return;
                }

                double distMeters = distanceMeters(prevLat, prevLon, lat, lon);
                if(source == LocationPing.PingSource.STUDENT && distMeters > 300.0){
                    return;
                }

                double dtSeconds = Math.max(0.0, (nowMs - prevTs) / 1000.0);
                if(distMeters > 50.0 + 33.0 * dtSeconds){
                    return;
                }

                if(!shouldReplaceLive(current, source, accuracy, nowMs, prevTs)){
                    return;
                }
            }

            String accStr = accuracy != null ? String.valueOf(accuracy) : "100.0";
            redisTemplate.opsForHash().putAll(liveKey, Map.of(
                "lat", String.valueOf(lat),
                "lon", String.valueOf(lon),
                "speed", String.valueOf(speedKmh),
                "accuracy", accStr,
                "ts", String.valueOf(nowMs),
                "source", source.name()
            ));
            redisTemplate.expire(liveKey, Duration.ofSeconds(60));

            writeDb = redisTemplate.opsForValue().setIfAbsent("trip:" + trip.getId() + ":db_throttle", "1", Duration.ofSeconds(6));
        }
            catch (Exception e){
            log.warn("Redis error during ping for trip {}: {}", trip.getId(), e.getMessage());
        }

        if(Boolean.TRUE.equals(writeDb)){
            LocationPing ping = new LocationPing();
            ping.setTrip(trip);
            ping.setRecordedAt(OffsetDateTime.now(clock));
            ping.setLatitude(lat);
            ping.setLongitude(lon);
            ping.setSpeed(speedKmh);
            ping.setAccuracy(accuracy);
            ping.setSource(source);
            ping.setSourceUserId(userId);
            locationRepository.save(ping);
        }
        
        checkStopArrival(trip, lat, lon);
    }
    private boolean shouldReplaceLive(
        Map<Object, Object> current,
        LocationPing.PingSource newSource,
        Double newAccuracy,
        long nowMs,
        long prevTs
    ){
        if((nowMs - prevTs) > 10_000L){
            return true;
        }
        String prevSource = (String) current.get("source");
        double prevAcc = Double.parseDouble((String) current.get("accuracy"));
        double newAcc = newAccuracy != null ? newAccuracy : 100.0;

        if("DRIVER".equals(prevSource) && prevAcc < 50.0 && newSource == LocationPing.PingSource.STUDENT){
            return false;
        }

        if(newSource == LocationPing.PingSource.DRIVER && newAcc < 50.0){
            return true;
        }
        return newAcc < prevAcc;
    }

    private void checkStopArrival(Trip trip, double lat, double lon){
        boolean evening = trip.getTripType() == Trip.TripType.EVENING;
        List<RouteStop> stops = routeService.getRouteStops(trip.getRoute().getId(), evening);
        Set<Long> arrivedIds = getArrivedStopIds(trip.getId());

        int nextIdx = 0;
        for (int i = stops.size() - 1; i >= 0; i--){
            if(arrivedIds.contains(stops.get(i).getStop().getId())){
                nextIdx = i + 1;
                break;
            }
        }
        List<RouteStop> nextThree = stops.subList(nextIdx, Math.min(nextIdx + 3, stops.size()));

        for(RouteStop candidate : nextThree){
            double dist = distanceMeters(lat, lon, candidate.getStop().getLatitude(), candidate.getStop().getLongitude());
            if(dist <= 80.0){
                entityManager.createNativeQuery(
                    "INSERT INTO stop_arrival (trip_id, stop_id, arrived_at) "
                    + "VALUES (:tid, :sid, :at) ON CONFLICT DO NOTHING"
                )
                .setParameter("tid", trip.getId())
                .setParameter("sid", candidate.getStop().getId())
                .setParameter("at", OffsetDateTime.now(clock))
                .executeUpdate();

                try{
                    redisTemplate.delete("trip:" + trip.getId() + ":arrived");
                }catch (Exception e){
                    log.debug("Redis evict failed for trip {} arrived stops: {}", trip.getId(), e.getMessage());
                }
                break;
            }
        }
    }

    private Set<Long> getArrivedStopIds(Long tripId){
        String key = "trip:" + tripId + ":arrived";
        try{
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null){
                Set<Long> ids = new HashSet<>();
                if (!cached.isBlank()){
                    for (String part : cached.split(",")){
                        ids.add(Long.parseLong(part));
                    }
                }
                return ids;
        }
    }catch (Exception e){
        log.debug("Redis read failed for {}: {}", key, e.getMessage());
    }

    @SuppressWarnings("unchecked")
    List<Number> arrived = entityManager.createNativeQuery("SELECT stop_id FROM stop_arrival WHERE trip_id = :tid")
        .setParameter("tid", tripId)
        .getResultList();

    Set<Long> arrivedIds = new HashSet<>();
    for(Number n : arrived) arrivedIds.add(n.longValue());

    try{
        String joined = arrivedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        redisTemplate.opsForValue().set(key, joined, Duration.ofSeconds(60));
    }catch (Exception e){
        log.debug("Redis write failed for {}: {}", key, e.getMessage());
    }
    return arrivedIds;
}

    @Transactional(readOnly = true)
    public LiveLocationResponse getLiveLocationByRoute(Long routeId){
        Trip trip = tripRepository.findByRouteIdAndStatus(routeId, Trip.TripStatus.RUNNING).orElse(null);
        return trip != null ? getLiveLocation(trip.getId()) : null;
    }

    @Transactional(readOnly = true)
    public LiveLocationResponse getLiveLocation(Long tripId){
        if (tripId == null) return null;
        try{
        Map<Object, Object> data = redisTemplate.opsForHash().entries("trip:" + tripId + ":live");

        if(!data.isEmpty()){

        long ts = Long.parseLong((String) data.get("ts"));
        long ageSeconds = Math.max(0, (clock.millis() - ts) / 1000L);
        return new LiveLocationResponse(
            Double.parseDouble((String) data.get("lat")),
            Double.parseDouble((String) data.get("lon")),
            Double.parseDouble((String) data.get("speed")),
            Double.parseDouble((String) data.get("accuracy")),
            (String) data.get("source"),
            ageSeconds,
            ageSeconds > 20
        );
    }
    return null;
        } catch (Exception e){
            log.warn("Redis live read failed for trip {}, falling back to DB: {}", tripId, e.getMessage());
            return locationRepository.findTopByTripIdOrderByRecordedAtDesc(tripId).map(p -> {
                long ageSeconds = Math.max(0, (clock.millis() - p.getRecordedAt().toInstant().toEpochMilli()) / 1000L);
                return new LiveLocationResponse(
                    p.getLatitude(),
                    p.getLongitude(),
                    p.getSpeed() != null ? p.getSpeed() : 0.0,
                    p.getAccuracy() != null ? p.getAccuracy() : 100.0,
                    p.getSource().name(),
                    ageSeconds,
                    ageSeconds > 20
                );
            })
            .orElse(null);
        }
        }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupOldPings(){
        locationRepository.deleteByRecordedAtBefore(OffsetDateTime.now(clock).minusDays(30));
    }

    public static double toKmh(Double speedMps) {
        return speedMps != null && speedMps > 0 ? speedMps * 3.6 : 0.0;
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2){
        double r = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record LiveLocationResponse(
        double lat,
        double lon,
        double speedKmh,
        double accuracy,
        String source,
        long ageSeconds,
        boolean stale
    ) {}
}