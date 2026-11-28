package com.bronzera.mybus.bus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buses")
public class BusController {

    private final BusService busService;

    public BusController(BusService busService){
        this.busService = busService;
    }

    @GetMapping
    public List<BusResponse> listBuses(
        @RequestParam Long collegeId,
        @RequestParam(defaultValue = "false") boolean includeInactive
    ){
        List<Bus> buses = includeInactive
        ? busService.listAllBuses(collegeId)
        : busService.listActiveBuses(collegeId);
        return buses.stream().map(BusResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<BusResponse> createBus(@Valid @RequestBody CreateBusRequest request){
        Bus bus = busService.createBus(
            request.collegeId(),
            request.busNumber(),
            request.registrationNumber(),
            request.busType(),
            request.photoUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BusResponse.from(bus));
    }

    @PatchMapping("/{id}/active")
    public BusResponse setActive(@PathVariable Long id, @RequestParam boolean active){
        return BusResponse.from(busService.setActive(id, active));
    }

    public record CreateBusRequest(
        @NotNull Long collegeId,
        @NotBlank @Size(max = 30) String busNumber,
        @NotBlank @Size(max = 30) String registrationNumber,
        @NotBlank @Pattern(regexp = "AC|NON_AC" , message = "busType must be AC or NON_AC") String busType,
        @Size(max = 500) String photoUrl
    ){}

    public record  BusResponse(
        Long id,
        Long collegeId,
        String busNumber,
        String registrationNumber,
        String busType,
        String photoUrl,
        boolean active
    ){
        public static BusResponse from(Bus bus){
            return new BusResponse(
                bus.getId(),
                bus.getCollege().getId(),
                bus.getBusNumber(),
                bus.getRegistrationNumber(),
                bus.getBusType(),
                bus.getPhotoUrl(),
                bus.isActive()
            );
        }
    }
}
