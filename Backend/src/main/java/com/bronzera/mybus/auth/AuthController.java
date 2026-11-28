package com.bronzera.mybus.auth;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController{
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/driver/login")
    public AuthService.DriverLoginResponse driverLogin(@Valid @RequestBody DriverLoginRequest request){
        return authService.driverLogin(request.loginId(), request.password(), request.deviceId());
    }

    @PostMapping("/driver/change-password")
    public void driverChangePassword(@Valid @RequestBody DriverChangePasswordRequest request){
        authService.driverChangePassword(
            request.driverId(),
            request.dob(),
            request.oldPassword(),
            request.newPassword()
        );
    }

    @PostMapping("/admin/login")
    public AuthService.AdminLoginResponse adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return authService.adminLogin(request.email(), request.password());
    }

    @PostMapping("/student/lookup")
    public AuthService.StudentPreviewResponse StudentLookup(@Valid @RequestBody StudentLookupRequest request){
        return authService.studentLookup(request.collegeId(), request.regNo(), request.dob());
    }

    @PostMapping("/student/send-otp")
    public void sendStudentOtp(@Valid @RequestBody SendOtpRequest request){
        authService.sendStudentOtp(request.lookupToken(), request.email());
    }

    @PostMapping("/student/verify-otp")
    public AuthService.StudentLoginResponse verifyStudentOtp(@Valid @RequestBody VerifyOtpRequest request){
        return authService.verifyStudentOtp(request.lookupToken(), request.email(), request.code());
    }

    public record DriverLoginRequest(
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank @Size(max = 200) String password,
        @Size(max = 200) String deviceId
    ) {}

    public record DriverChangePasswordRequest(
        @NotNull Long driverId,
        @NotNull LocalDate dob,
        @NotBlank @Size(max = 100) String oldPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record AdminLoginRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 100) String password
    ) {}

    public record StudentLookupRequest(
        @NotNull Long collegeId,
        @NotBlank @Size(max = 50) String regNo,
        @NotNull LocalDate dob 
    ) {}

    public record SendOtpRequest(
        @NotBlank String lookupToken,
        @NotBlank @Email @Size(max = 200) String email
    ) {}

    public record VerifyOtpRequest(
        @NotBlank String lookupToken,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 10) String code
    ) {}
}