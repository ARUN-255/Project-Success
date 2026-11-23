package com.bronzera.mybus.driver;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

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
    public List<Driver> listDrivers(Long collegeId){
        return driverRepository.findByCollegeId(collegeId);
    }

    @Transactional
    public Driver createDriver(Long collegeId, String loginId, String name, String password, LocalDate dob){

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
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver resetPassword(Long id, LocalDate dob, String newPassword){
        if (dob == null || newPassword == null || newPassword.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dob and newPassword are required");
        }
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        if (!driver.getDob().equals(dob)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth does not match");
        }
        driver.setPasswordHash(passwordEncoder.encode(newPassword));
        driver.setMustChangePassword(true);
        driver.setFailedAttempts(0);
        driver.setLockedUntil(null);
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver setActive(Long id, boolean active){
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setActive(active);
        return driverRepository.save(driver);
    }
}