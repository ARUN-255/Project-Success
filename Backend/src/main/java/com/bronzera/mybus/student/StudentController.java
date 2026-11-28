package com.bronzera.mybus.student;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController{

    private final StudentService studentService;
    private final StudentUploadService studentUploadService;

    public StudentController(StudentService studentService, StudentUploadService studentUploadService){
        this.studentService = studentService;
        this.studentUploadService = studentUploadService;
    }

    @GetMapping
    public Page<StudentService.StudentResponse> listStudents(
        @RequestParam Long collegeId,
        @RequestParam(required = false) String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ){
        return studentService.listStudents(collegeId, department, page, size);
    }

    @PostMapping
    public ResponseEntity<StudentService.StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request){
        StudentService.StudentResponse student = studentService.createStudent(
            request.collegeId(),
            request.regNo(),
            request.name(),
            request.address(),
            request.department(),
            request.year(),
            request.dob()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(student);
    }

    @PostMapping("/upload")
    public StudentUploadService.UploadResult uploadCsv(
        @RequestParam Long collegeId,
        @RequestParam(defaultValue = "true") boolean dryRun,
        @RequestParam("file") MultipartFile file
    ){
        return studentUploadService.processCsv(collegeId, file, dryRun);
    }

    @PatchMapping("/{id}/active")
    public StudentService.StudentResponse setActive(@PathVariable Long id, @RequestParam boolean active){
        return studentService.setActive(id, active);
    }

    @PostMapping("/{id}/reset")
    public StudentService.StudentResponse resetStudent(@PathVariable Long id){
        return studentService.resetStudent(id);
    }

    @PatchMapping("/{id}/share-location")
    public StudentService.StudentResponse setShareLocation(@PathVariable Long id, @RequestParam boolean allowed){
        return studentService.setShareLocationAllowed(id, allowed);
    }

    @GetMapping("/{id}/route-pref")
    public List<StudentService.RoutePrefResponse> getRoutePref(@PathVariable Long id){
        return studentService.getRoutePreferences(id);
    }

    @PutMapping("/{id}/route-pref")
    public ResponseEntity<Void> saveRoutePref(
        @PathVariable Long id,
        @Valid @RequestBody RoutePrefRequest request 
    ){
        studentService.saveRoutePreference(id, request.routeId(), request.stopId(), request.tripType());
        return ResponseEntity.noContent().build();
    }

    public record RoutePrefRequest(
        @NotNull Long routeId,
        @NotNull Long stopId,
        @NotNull String tripType 
    ) {}

    public record CreateStudentRequest(
        @NotNull Long collegeId,
        @NotBlank @Size(max = 50) String regNo,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String department,
        @Min(1) @Max(4) int year,
        @NotNull LocalDate dob
    ) {}

}