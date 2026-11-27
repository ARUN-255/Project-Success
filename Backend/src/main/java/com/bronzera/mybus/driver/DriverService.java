package com.bronzera.mybus.driver;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.time.LocalDate;


@Service
public class DriverService{
    private final DriverRepository driverRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverService(
        DriverRepository driverRepository,
        CollegeRepository collegeRepository,
        PasswordEncoder passwordEncoder
    ){
        this.driverRepository = driverRepository;
        this.collegeRepository = collegeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<DriverResponse> listDrivers(Long collegeId, int page, int size){
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return driverRepository.findByCollegeId(
            collegeId, PageRequest.of(safePage, safeSize, Sort.by("id").ascending())
        ).map(DriverResponse::from);
    }

    @Transactional
    public DriverResponse createDriver(Long collegeId, String loginId, String name, String password, LocalDate dob){

        if(loginId == null || loginId.isBlank() || name == null || name.isBlank() || password == null || password.isBlank() || dob == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "loginId, name, password, and dob are required");
        }

        if(driverRepository.existsByLoginIdIgnoreCase(loginId.trim())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver ID already exists");
        }
        College college = collegeRepository.findById(collegeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        Driver driver = new Driver();
        driver.setCollege(college);
        driver.setLoginId(loginId.trim());
        driver.setName(name.trim());
        driver.setPasswordHash(passwordEncoder.encode(password));
        driver.setDob(dob);
        driver.setMustChangePassword(true);
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public DriverResponse resetPassword(Long id, LocalDate dob, String newPassword){
        if(dob == null || newPassword == null || newPassword.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dob and newPassword are required");
        }
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        if(driver.getLockedUntil() != null && driver.getLockedUntil().isAfter(OffsetDateTime.now())){
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later");
        }

        if(!driver.getDob().equals(dob)){
            int attempts = driver.getFailedAttempts() +1;
            driver.setFailedAttempts(attempts);
            if(attempts >= 5){
                driver.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
            }
            driverRepository.save(driver);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth does not match");
        }

        driver.setPasswordHash(passwordEncoder.encode(newPassword));
        driver.setMustChangePassword(true);
        driver.setFailedAttempts(0);
        driver.setLockedUntil(null);
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse setActive(Long id, boolean active){
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setActive(active);
        return DriverResponse.from(driverRepository.save(driver));
    }

    public record DriverResponse(
        Long id,
        Long collegeId,
        String loginId,
        String name,
        boolean mustChangePassword,
        boolean active 
    ){
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