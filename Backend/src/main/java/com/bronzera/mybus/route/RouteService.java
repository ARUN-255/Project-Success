package com.bronzera.mybus.route;

import com.bronzera.mybus.bus.Bus;
import com.bronzera.mybus.bus.BusRepository;
import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

@Service
public class RouteService {
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final CollegeRepository collegeRepository;
    private final EntityManager entityManager;

    public RouteService(
        RouteRepository routeRepository,
        BusRepository busRepository,
        CollegeRepository collegeRepository,
        EntityManager entityManager){

            this.routeRepository = routeRepository;
            this.busRepository = busRepository;
            this.collegeRepository = collegeRepository;
            this.entityManager = entityManager;
    }
    
    public List<Route> listRoutesByCollege(Long collegeId){
        return routeRepository.findByBusCollegeIdAndActiveTrue(collegeId);
    }

    @Transactional
    public Stop createStop(Long collegeId, String name, double latitude, double longitude){
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
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Bus not found"));

        Route route = new Route();
        route.setBus(bus);
        route.setName(name.trim());
        route = routeRepository.save(route);

        if (stops != null){
            for (int i = 0; i < stops.size(); i++){
                StopTimeInput item = stops.get(i);
                Stop stop = entityManager.find(Stop.class, item.stopId());
                if (stop == null){
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found: " + item.stopId());
                }
                RouteStop rs = new RouteStop();
                rs.setRoute(route);
                rs.setStop(stop);
                rs.setSequenceOrder(i + 1);
                rs.setMorningTime(item.morningTime());
                rs.setEveningTime(item.eveningTime());
                entityManager.persist(rs);
            }
        }
        return route;
    }
    public List<RouteStop> getRouteStops(Long routeId, boolean eveningOrder){
        String order = eveningOrder ? "DESC" : "ASC";
        return entityManager.createQuery(
            "SELECT rs FROM RouteStop rs JOIN FETCH rs.stop WHERE rs.route.id = :routeId ORDER BY rs.sequenceOrder " + order, RouteStop.class).setParameter("routeId", routeId).getResultList();
    }
    public record StopTimeInput(Long stopId, LocalTime morningTime, LocalTime eveningTime) {}
}