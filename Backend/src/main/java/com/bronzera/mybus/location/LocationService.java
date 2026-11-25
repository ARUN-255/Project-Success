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

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LocationService{

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
    public void recordDriverPing(Long tripId, Long driverId, double lat, double lon, Double speedMps, Double accuracy){
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if(trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the trip driver");
        }
        processPing(trip, LocationPing.PingSource.DRIVER, driverId, lat, lon, speedMps, accuracy);
    }

    @Transactional
    public void recordStudentPing(Long routeId, Long studentId, double lat, double lon, Double speedMps, Double accuracy){
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

            processPing(trip, LocationPing.PingSource.STUDENT, studentId, lat, lon, speedMps, accuracy);
    }

    private void processPing(
        Trip trip,
        LocationPing.PingSource source,
        Long userId,
        double lat,
        double lon,
        Double speedMps,
        Double accuracy
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
        Map<Object, Object> current = redisTemplate.opsForHash().entries(liveKey);

        if(!current.isEmpty()){
            double prevLat = Double.parseDouble((String) current.get("lat"));
            double prevLon = Double.parseDouble((String) current.get("lon"));
            long prevTs = Long.parseLong((String) current.get("ts"));
            double distMeters = distanceMeters(prevLat, prevLon, lat, lon);

            if (source == LocationPing.PingSource.STUDENT && distMeters > 300.0){
                return;
            }

            double dtHours = (nowMs - prevTs) / 3_600_000.0;
            if(dtHours > 0 && (distMeters / 1000.0) / dtHours > 120.0){
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

        Boolean writeDb = redisTemplate.opsForValue().setIfAbsent("trip:" + trip.getId() + ":db_throttle", "1", Duration.ofSeconds(6));

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
        List<RouteStop> stops = routeService.getRouteStops(trip.getRoute().getId(),evening);
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (RouteStop rs : stops){
            double dist = distanceMeters(lat, lon, rs.getStop().getLatitude(),rs.getStop().getLongitude());

            if(dist <= 80.0){
                entityManager.createNativeQuery("INSERT INTO stop_arrival (trip_id, stop_id, arrived_at) " + "VALUES (:tid, :sid, :at) ON CONFLICT DO NOTHING")
                .setParameter("tid", trip.getId())
                .setParameter("sid", rs.getStop().getId())
                .setParameter("at",now)
                .executeUpdate();
            }
        }
    }
    public LiveLocationResponse getLiveLocation(Long tripId){
        Map<Object, Object> data = redisTemplate.opsForHash().entries("trip:" + tripId + ":live");

        if(data.isEmpty()){
            return null;
        }

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