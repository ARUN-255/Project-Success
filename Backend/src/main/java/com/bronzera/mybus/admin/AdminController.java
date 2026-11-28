package com.bronzera.mybus.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminService.AdminResponse> listAdmins(@RequestParam(required = false) Long collegeId) {
        return adminService.listAdmins(collegeId);
    }

    @PostMapping
    public ResponseEntity<AdminService.AdminResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createAdmin(request.collegeId(), request.email(), request.password(), request.owner()));
    }

    @PatchMapping("/{id}/active")
    public AdminService.AdminResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return adminService.setActive(id, active);
    }

    @PostMapping("/{id}/request-owner-access")
    public AdminService.AdminResponse requestOwnerAccess(@PathVariable Long id) {
        return adminService.requestOwnerAccess(id);
    }

    public record CreateAdminRequest(
        Long collegeId,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        boolean owner
    ) {}
}
