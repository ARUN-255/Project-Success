package com.bronzera.mybus.driver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService){
        this.driverService = driverService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers(@RequestParam Long collegeId){
        return driverService.listDrivers(collegeId).stream().map(DriverResponse::from).toList();
    }
    
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@RequestBody CreateDriverRequest request){
        Driver driver = driverService.createDriver(
            request.collegeId(),
            request.loginId(),
            request.name(),
            request.password(),
            request.dob()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DriverResponse.from(driver));
    }

    @PostMapping("/{id}/reset-password")
    public DriverResponse resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest  request){
        return DriverResponse.from(driverService.resetPassword(id, request.dob(), request.newPassword()));
    }

    @PatchMapping("/{id}/active")
    public DriverResponse setActive(@PathVariable Long id, @RequestParam boolean active){
        return DriverResponse.from(driverService.setActive(id, active));
    }

    public record CreateDriverRequest(Long collegeId, String loginId, String name, String password, LocalDate dob) {}

    public record ResetPasswordRequest(LocalDate dob, String newPassword) {}

    public record DriverResponse(
        Long id,
        Long collegeId,
        String loginId,
        String name,
        boolean mustChangePassword,
        boolean active 
    ) {
        public static DriverResponse from(Driver d){
            return new DriverResponse(
                d.getId(),
                d.getCollege().getId(),
                d.getLoginId(),
                d.getName(),
                d.isMustChangePassword(),
                d.isActive()
            );
        }
    }
}