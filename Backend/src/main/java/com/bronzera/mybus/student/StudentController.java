package com.bronzera.mybus.student;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController{

    private final StudentService studentService;
    private final StudentUploadService studentUploadService;

    public StudentController(StudentService studentService, StudentUploadService studentUploadService){
        this.studentService = studentService;
        this.studentUploadService = studentUploadService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(
        @RequestParam Long collegeId,
        @RequestParam(required = false) String department
    ){
        return studentService.listStudents(collegeId, department).stream().map(StudentResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@RequestBody CreateStudentRequest request){
        Student student = studentService.createStudent(
            request.collegeId(),
            request.regNo(),
            request.name(),
            request.address(),
            request.department(),
            request.year(),
            request.dob()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(StudentResponse.from(student));
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
    public StudentResponse setActive(@PathVariable Long id, @RequestParam boolean active){
        return StudentResponse.from(studentService.setActive(id, active));
    }

    @PostMapping("/{id}/reset")
    public StudentResponse resetStudent(@PathVariable Long id){
        return StudentResponse.from(studentService.resetStudent(id));
    }

    public record CreateStudentRequest(
        Long collegeId,
        String regNo,
        String name,
        String address,
        String department,
        int year,
        LocalDate dob
    ) {}

    public record StudentResponse(
        Long id,
        Long collegeId,
        String regNo,
        String name,
        String address,
        String department,
        int year,
        LocalDate dob,
        String collegeEmail,
        boolean active 
    ){
        public static StudentResponse from(Student s){
            return new StudentResponse(
                s.getId(),
                s.getCollege().getId(),
                s.getRegNo(),
                s.getName(),
                s.getAddress(),
                s.getDepartment(),
                s.getYear(),
                s.getDob(),
                s.getCollegeEmail(),
                s.isActive()
            );
        }
    }
}