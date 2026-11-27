package com.bronzera.mybus.driver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService){
        this.driverService = driverService;
    }

    @GetMapping
    public Page<DriverService.DriverResponse> listDrivers(
        @RequestParam Long collegeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size 
    ){
        return driverService.listDrivers(collegeId, page, size);
    }
    
    @PostMapping
    public ResponseEntity<DriverService.DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request){
        DriverService.DriverResponse driver = driverService.createDriver(
            request.collegeId(),
            request.loginId(),
            request.name(),
            request.password(),
            request.dob()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(driver);
    }

    @PostMapping("/{id}/reset-password")
    public DriverService.DriverResponse resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request){
        return driverService.resetPassword(id, request.dob(), request.newPassword());
    }

    @PatchMapping("/{id}/active")
    public DriverService.DriverResponse setActive(@PathVariable Long id, @RequestParam boolean active){
        return driverService.setActive(id, active);
    }

    public record CreateDriverRequest(
        @NotNull Long collegeId,
        @NotBlank String loginId, 
        @NotBlank String name, 
        @NotBlank @Size(min = 8) String password, 
        @NotNull LocalDate dob) {}

    public record ResetPasswordRequest(
        @NotNull LocalDate dob, 
        @NotBlank @Size(min = 8) String newPassword) {}

    
}