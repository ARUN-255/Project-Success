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

import jakarta.validation.constraints.NotNull;

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
     if(days == null || days.isEmpty()){
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Days list is required");
     }
     if(days.size() > 62){
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot upsert more than 62 days at once");
     }

     Route route = routeRepository.findById(routeId)
         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

     return days.stream().map(day -> {
         if(day == null || day.runDate() == null){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each day must include runDate");
         }
         if((day.morningStart() == null) != (day.morningEnd() == null)){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both morningStart and morningEnd are required");
         }
         if(day.morningStart() != null && !day.morningStart().isBefore(day.morningEnd())){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "morningStart must be before morningEnd");
         }
         if((day.eveningStart() == null) != (day.eveningEnd() == null)){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both eveningStart and eveningEnd are required");
         }
         if(day.eveningStart() != null && !day.eveningStart().isBefore(day.eveningEnd())){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eveningStart must be before eveningEnd");
         }
         if(day.running() && day.morningStart() == null && day.eveningStart() == null){
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one trip window is required when running is true");
         }

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

    @Transactional(readOnly = true)
    public List<RunSchedule> listSchedules(Long routeId, LocalDate start, LocalDate end){
        if(end.isBefore(start)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end date must not be before start date");
        }
        return scheduleRepository.findByRouteIdAndRunDateBetweenOrderByRunDateAsc(routeId, start, end);
    }


    @Transactional(readOnly = true)
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
        @NotNull LocalDate runDate,
        LocalTime morningStart,
        LocalTime morningEnd,
        LocalTime eveningStart,
        LocalTime eveningEnd,
        boolean running
    ) {}
}