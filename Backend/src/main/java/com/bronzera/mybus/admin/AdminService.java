package com.bronzera.mybus.admin;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService{

    private final AdminRepository adminRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
        AdminRepository adminRepository,
        CollegeRepository collegeRepository,
        PasswordEncoder passwordEncoder
    ){
        this.adminRepository = adminRepository;
        this.collegeRepository = collegeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> listAdmins(Long collegeId){
        List<Admin> admins = (collegeId != null)
        ? adminRepository.findByCollegeId(collegeId)
        : adminRepository.findAll();
        return admins.stream().map(AdminResponse::from).toList();
    }

    @Transactional
    public AdminResponse createAdmin(Long collegeId, String email, String password, boolean owner) {
        if (!owner && collegeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "collegeId is required for non-owner admins");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (adminRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin email already exists");
        }
        College college = null;
        if (collegeId != null) {
            college = collegeRepository.findById(collegeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));
        }

        Admin admin = new Admin();
        admin.setCollege(college);
        admin.setEmail(cleanEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setOwner(owner);
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional
    public AdminResponse setActive(Long id, boolean active) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        admin.setActive(active);
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional
    public AdminResponse requestOwnerAccess(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        admin.setOwnerAccessRequested(true);
        return AdminResponse.from(adminRepository.save(admin));
    }

    public record AdminResponse(
        Long id,
        Long collegeId,
        String email,
        boolean owner,
        boolean ownerAccessRequested,
        boolean active
    ) {
        public static AdminResponse from(Admin a) {
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
