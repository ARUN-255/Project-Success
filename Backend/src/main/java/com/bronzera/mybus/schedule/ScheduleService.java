package com.bronzera.mybus.schedule;

import com.bronzera.mybus.route.Route;
import com.bronzera.mybus.route.RouteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleService{
    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, RouteRepository routeRepository){
        this.scheduleRepository = scheduleRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional 
    public List<RunSchedule> bulkUpsertSchedules(Long routeId, List<ScheduleDayInput> days){
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        return days.stream().map(day -> {
            RunSchedule schedule = scheduleRepository.findByRouteIdAndRunDate(routeId, day.runDate()).orElseGet(() -> {
                RunSchedule s = new RunSchedule();
                s.setRoute(route);
                s.setRunDate(day.runDate());
                return s;
            });
        
        schedule.setMorningStart(day.morningStart());
        schedule.setMorningEnd(day.morningEnd());
        schedule.setEveningStart(day.eveningStart());
        schedule.setEveningEnd(day.eveningEnd());
        schedule.setRunning(day.running());
        return scheduleRepository.save(schedule);
        }).toList();
    }
    public List<RunSchedule> listSchedules(Long routeId, LocalDate start, LocalDate end){
        return scheduleRepository.findByRouteIdAndRunDateBetweenOrderByRunDateAsc(routeId, start, end);
    }
    public boolean isInsideActiveWindow(Long routeId, LocalDate date, LocalTime now, String tripType){
        return scheduleRepository.findByRouteIdAndRunDate(routeId, date).filter(RunSchedule :: isRunning).map(s -> {
            if("MORNING".equals(tripType)){
                return s.getMorningStart() != null && s.getMorningEnd() != null && !now.isBefore(s.getMorningStart()) && !now.isAfter(s.getMorningEnd());
            }
            if("EVENING".equals(tripType)){
                return s.getEveningStart() !=null && s.getEveningEnd() != null && !now.isBefore(s.getEveningStart()) && !now.isAfter(s.getEveningEnd());
            }
            return false;
        }).orElse(false);
    }
    public record ScheduleDayInput(
        LocalDate runDate,
        LocalTime morningStart,
        LocalTime morningEnd,
        LocalTime eveningStart,
        LocalTime eveningEnd,
        boolean running
    ) {}
}