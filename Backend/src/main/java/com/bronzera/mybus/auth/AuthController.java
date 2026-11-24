package com.bronzera.mybus.auth;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
public class AuthController{
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/driver/login")
    public AuthService.DriverLoginResponse driverLogin(@RequestBody DriverLoginRequest request){
        return authService.driverLogin(request.loginId(), request.password(), request.deviceId());
    }

    @PostMapping("/driver/change-password")
    public void driverChangePassword(@RequestBody DriverChangePasswordRequest request){
        authService.driverChangePassword(
            request.driverId(),
            request.dob(),
            request.oldPassword(),
            request.newPassword()
        );
    }

    @PostMapping("/admin/login")
    public AuthService.AdminLoginResponse adminLogin(@RequestBody AdminLoginRequest request) {
        return authService.adminLogin(request.email(), request.password());
    }

    @PostMapping("/student/lookup")
    public AuthService.StudentPreviewResponse StudentLookup(@RequestBody StudentLookupRequest request){
        return authService.studentLookup(request.collegeId(), request.regNo(), request.dob());
    }

    @PostMapping("/student/send-otp")
    public void sendStudentOtp(@RequestBody SendOtpRequest request){
        authService.sendStudentOtp(request.studentId(), request.email());
    }

    @PostMapping("/student/verify-otp")
    public AuthService.StudentLoginResponse verifyStudentOtp(@RequestBody VerifyOtpRequest request){
        return authService.verifyStudentOtp(request.studentId(), request.email(), request.code());
    }

    public record DriverLoginRequest(String loginId, String password, String deviceId) {}
    public record DriverChangePasswordRequest(Long driverId, LocalDate dob, String oldPassword, String newPassword) {}
    public record AdminLoginRequest(String email, String password) {}
    public record StudentLookupRequest(Long collegeId, String regNo, LocalDate dob) {}
    public record SendOtpRequest(Long studentId, String email) {}
    public record VerifyOtpRequest(Long studentId, String email, String code) {}
}