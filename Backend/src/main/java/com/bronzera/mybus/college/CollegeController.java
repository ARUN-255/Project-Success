package com.bronzera.mybus.college;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController{

    private final CollegeRepository collegeRepository;

    public CollegeController(CollegeRepository collegeRepository){
        this.collegeRepository = collegeRepository;
    }

    @GetMapping
    public List<College> listColleges(){
        return collegeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<College> getCollege(@PathVariable Long id){
        return collegeRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCollege(@Valid @RequestBody CreateCollegeRequest request){
        if (request.name() == null || request.name().isBlank()
        || request.emailDomain() == null || request.emailDomain().isBlank()){
            return ResponseEntity.badRequest().body("Name and emailDomain are required");
        }
        if (collegeRepository.existsByNameIgnoreCase(request.name().trim())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("College already  exists");
        }
        College college = new College();
        college.setName(request.name().trim());
        college.setEmailDomain(request.emailDomain().trim().toLowerCase());
        return ResponseEntity.status(HttpStatus.CREATED).body(collegeRepository.save(college));
    }
    public record CreateCollegeRequest(@NotBlank String name, @NotBlank String emailDomain) {}
}