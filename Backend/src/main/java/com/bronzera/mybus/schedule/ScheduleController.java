package com.bronzera.mybus.schedule;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService){
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleResponse> listSchedules(
        @RequestParam Long routeId,
        @RequestParam LocalDate start,
        @RequestParam LocalDate end){
            return scheduleService.listSchedules(routeId, start, end).stream().map(ScheduleResponse::from).toList();
        }
        @PostMapping
        public List<ScheduleResponse> upsertSchedules(@Valid @RequestBody UpsertSchedulesRequest request){
            return scheduleService.bulkUpsertSchedules(request.routeId(), request.days()).stream().map(ScheduleResponse::from).toList();
        }
        public record UpsertSchedulesRequest( @NotNull Long routeId, @NotEmpty @Size(max = 62) @Valid List<ScheduleService.ScheduleDayInput> days){}

        public record ScheduleResponse(
            Long id,
            Long routeId,
            LocalDate runDate,
            LocalTime morningStart,
            LocalTime morningEnd,
            LocalTime eveningStart,
            LocalTime eveningEnd,
            boolean running
        ){
            public static ScheduleResponse from(RunSchedule s){
                return new ScheduleResponse(
                    s.getId(),
                    s.getRoute().getId(),
                    s.getRunDate(),
                    s.getMorningStart(),
                    s.getMorningEnd(),
                    s.getEveningStart(),
                    s.getEveningEnd(),
                    s.isRunning()
                );
            }
        }
}