package com.bronzera.mybus.bus;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusService {
    private final BusRepository busRepository;
    private final CollegeRepository collegeRepository;

    public BusService(BusRepository busRepository, CollegeRepository collegeRepository){
        this.busRepository = busRepository;
        this.collegeRepository = collegeRepository;
    }
    
    @Transactional(readOnly = true)
    public List<Bus> listActiveBuses(Long collegeId){
        return busRepository.findByCollegeIdAndActiveTrue(collegeId);
    }

    @Transactional(readOnly = true)
    public List<Bus> listAllBuses(Long collegeId){
        return busRepository.findByCollegeId(collegeId);
    }

    @Transactional
    public Bus createBus(Long collegeId, String busNumber, String registrationNumber, String busType, String photoUrl){
        if (busNumber == null || busNumber.isBlank() || registrationNumber == null || registrationNumber.isBlank() || busType == null || (!busType.equals("AC") && !busType.equals("NON_AC"))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid busNumber, registrationNumber, and busType (AC/NON_AC) are required");
        }
        if (busRepository.existsByCollegeIdAndBusNumberIgnoreCase(collegeId, busNumber.trim())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bus number already exists for this college");
        }

        College college = collegeRepository.findById(collegeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        Bus bus = new Bus();
        bus.setCollege(college);
        bus.setBusNumber(busNumber.trim().toUpperCase());
        bus.setRegistrationNumber(registrationNumber.trim().toUpperCase());
        bus.setBusType(busType);
        bus.setPhotoUrl(photoUrl);
        return busRepository.save(bus);
     }

     @Transactional
     public Bus setActive(Long busId, boolean active){
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus not found"));
        bus.setActive(active);
        return busRepository.save(bus);
     }
}