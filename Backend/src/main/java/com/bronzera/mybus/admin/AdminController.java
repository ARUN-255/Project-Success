package com.bronzera.mybus.admin;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admins")
public class AdminController{
    private final AdminRepository adminRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
        AdminRepository adminRepository,
        CollegeRepository collegeRepository,
        PasswordEncoder passwordEncoder){
            this.adminRepository = adminRepository;
            this.collegeRepository = collegeRepository;
            this.passwordEncoder = passwordEncoder;
        }

        @GetMapping
        @Transactional(readOnly = true)
        public List<AdminResponse> listAdmins(@RequestParam(required = false) Long collegeId){
            List<Admin> admins = (collegeId != null)
            ? adminRepository.findByCollegeId(collegeId)
            : adminRepository.findAll();
            return admins.stream().map(AdminResponse::from).toList();
        }

        @PostMapping
        public ResponseEntity<AdminResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request){
            if (request.email() == null || request.email().isBlank()|| request.password() == null || request.password().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
            }
            if (!request.owner() && request.collegeId() == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"collegeId is required for non-owner admins");
            }
            if (adminRepository.existsByEmailIgnoreCase(request.email().trim())){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Admin email already exists");
            }
            College college = null;
            if (request.collegeId() != null){
                college = collegeRepository.findById(request.collegeId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));
            }

            Admin admin = new Admin();
            admin.setCollege(college);
            admin.setEmail(request.email().trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(request.password()));
            admin.setOwner(request.owner());
            return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.from(adminRepository.save(admin)));
        }

        @PatchMapping("/{id}/active")
        public AdminResponse setActive(@PathVariable Long id, @RequestParam boolean active){
            Admin admin = adminRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
            admin.setActive(active);
            return AdminResponse.from(adminRepository.save(admin));
        }

        @PostMapping("/{id}/request-owner-access")
        public AdminResponse requestOwnerAccess(@PathVariable Long id){
            Admin admin = adminRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Admin not found"));
            admin.setOwnerAccessRequested(true);
            return AdminResponse.from(adminRepository.save(admin));
        }

        public record CreateAdminRequest(Long collegeId, @NotBlank @Email String email, @NotBlank String password, boolean owner) {}

        public record AdminResponse(
            Long id,
            Long collegeId,
            String email,
            boolean owner,
            boolean ownerAccessRequested,
            boolean active 
        ){
            public static AdminResponse from(Admin a){
                return new AdminResponse(
                    a.getId(),
                    a.getCollege() != null ? a.getCollege().getId() : null,
                    a.getEmail(),
                    a.isOwner(),
                    a.isOwnerAccessRequested(),
                    a.isActive()
                );
            }
        }
}