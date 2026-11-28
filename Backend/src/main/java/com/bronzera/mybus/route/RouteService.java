package com.bronzera.mybus.route;

import com.bronzera.mybus.bus.Bus;
import com.bronzera.mybus.bus.BusRepository;
import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalTime;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final CollegeRepository collegeRepository;
    private final EntityManager entityManager;
    private final StringRedisTemplate redisTemplate;

    public RouteService(
        RouteRepository routeRepository,
        BusRepository busRepository,
        CollegeRepository collegeRepository,
        EntityManager entityManager,
        StringRedisTemplate redisTemplate){

            this.routeRepository = routeRepository;
            this.busRepository = busRepository;
            this.collegeRepository = collegeRepository;
            this.entityManager = entityManager;
            this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public List<Route> listRoutesByCollege(Long collegeId){
        return routeRepository.findByBusCollegeIdAndActiveTrue(collegeId);
    }

    @Transactional
    public Stop createStop(Long collegeId, String name, Double latitude, Double longitude){
        if(name == null || name.isBlank() || latitude == null || longitude == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stop name, latitude, and longitude are required");
        }
        
        College college = collegeRepository.findById(collegeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        Stop stop = new Stop();
        stop.setCollege(college);
        stop.setName(name.trim());
        stop.setLatitude(latitude);
        stop.setLongitude(longitude);
        entityManager.persist(stop);
        return stop;
    }

    public List<Stop> listStopsByCollege(Long collegeId){
        return entityManager.createQuery(
            "SELECT s FROM Stop s WHERE s.college.id = :collegeId ORDER BY s.name", Stop.class).setParameter("collegeId", collegeId).getResultList();
    }

    @Transactional
    public Route createRoute(Long busId, String name, List<StopTimeInput> stops){
        
        if (name == null || name.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route name is required");
        }
        
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Bus not found"));

        Route route = new Route();
        route.setBus(bus);
        route.setName(name.trim());
        route = routeRepository.save(route);

        if (stops == null || stops.isEmpty()){
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one stop is required");
     }

     Set<Long> seenStopIds = new HashSet<>();
     LocalTime prevMorning = null;
     LocalTime prevEvening = null;

     for (int i = 0; i < stops.size(); i++){
         StopTimeInput item = stops.get(i);
         if (item == null || item.stopId() == null || item.morningTime() == null || item.eveningTime() == null){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each stop needs stopId, morningTime, and eveningTime");
         }
         if (!seenStopIds.add(item.stopId())){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate stop on route: " + item.stopId());
         }
         if (prevMorning != null && !item.morningTime().isAfter(prevMorning)){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Morning stop times must be strictly increasing");
         }
         if (prevEvening != null && !item.eveningTime().isBefore(prevEvening)){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evening stop times must increase in reverse stop order");
         }
         prevMorning = item.morningTime();
         prevEvening = item.eveningTime();

         Stop stop = entityManager.find(Stop.class, item.stopId());
         if (stop == null){
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found: " + item.stopId());
         }
         if(!stop.getCollege().getId().equals(bus.getCollege().getId())){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stop does not belong to the bus's college");
         }

         RouteStop rs = new RouteStop();
         rs.setRoute(route);
         rs.setStop(stop);
         rs.setSequenceOrder(i + 1);
         rs.setMorningTime(item.morningTime());
         rs.setEveningTime(item.eveningTime());
         entityManager.persist(rs);
     }
        evictRouteStopsCache(route.getId());
        return route;
    }
    
    @Transactional(readOnly = true)
    public List<RouteStop> getRouteStops(Long routeId, boolean eveningOrder){
        String key = "route:" + routeId + ":stops:" + (eveningOrder ? "desc" : "asc");
        try{
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null){
                return deserializeStops(cached);
            }
        }catch (Exception e){
            log.debug("Redis read failed for {}: {}", key, e.getMessage());
        }

        String order = eveningOrder ? "DESC" : "ASC";
        List<RouteStop> stops = entityManager.createQuery(
            "SELECT rs FROM RouteStop rs JOIN FETCH rs.stop WHERE rs.route.id = :routeId ORDER BY rs.sequenceOrder " + order, RouteStop.class)
            .setParameter("routeId", routeId)
            .getResultList();

        try{
            redisTemplate.opsForValue().set(key, serializeStops(stops), Duration.ofMinutes(30));
        }catch (Exception e){
            log.debug("Redis write failed for {}: {}", key, e.getMessage());
        }
        return stops;
    }

public void evictRouteStopsCache(Long routeId){
    try{
        redisTemplate.delete(List.of(
            "route:" + routeId + ":stops:asc",
            "route:" + routeId + ":stops:desc"
        ));
    }catch (Exception e){
        log.debug("Redis evict failed for route {}: {}", routeId, e.getMessage());
    }
}

private String serializeStops(List<RouteStop> stops){
    StringBuilder sb = new StringBuilder();
    for (RouteStop rs : stops){
        Stop s = rs.getStop();
        String encodedName = Base64.getEncoder().encodeToString(s.getName().getBytes(StandardCharsets.UTF_8));
        sb.append(s.getId()).append('|')
          .append(encodedName).append('|')
          .append(s.getLatitude()).append('|')
          .append(s.getLongitude()).append('|')
          .append(rs.getSequenceOrder()).append('|')
          .append(rs.getMorningTime() != null ? rs.getMorningTime().toString() : "").append('|')
          .append(rs.getEveningTime() != null ? rs.getEveningTime().toString() : "").append('\n');
    }
    return sb.toString();
}

private List<RouteStop> deserializeStops(String data){
    List<RouteStop> result = new ArrayList<>();
    if (data.isBlank()) return result;
    for (String line : data.split("\n")){
        if (line.isBlank()) continue;
        String[] p = line.split("\\|", -1);
        Stop s = new Stop();
        s.setId(Long.parseLong(p[0]));
        s.setName(new String(Base64.getDecoder().decode(p[1]), StandardCharsets.UTF_8));
        s.setLatitude(Double.parseDouble(p[2]));
        s.setLongitude(Double.parseDouble(p[3]));

        RouteStop rs = new RouteStop();
        rs.setStop(s);
        rs.setSequenceOrder(Integer.parseInt(p[4]));
        rs.setMorningTime(p[5].isEmpty() ? null : LocalTime.parse(p[5]));
        rs.setEveningTime(p[6].isEmpty() ? null : LocalTime.parse(p[6]));
        result.add(rs);
    }
    return result;
}

    public record StopTimeInput(@NotNull Long stopId, @NotNull LocalTime morningTime, @NotNull LocalTime eveningTime) {}
}