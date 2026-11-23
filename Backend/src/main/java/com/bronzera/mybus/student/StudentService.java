package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService{

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;

    public StudentService(StudentRepository studentRepository, CollegeRepository collegeRepository){
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents(Long collegeId, String department){
        if(department != null && !department.isBlank()){
            return studentRepository.findByCollegeIdAndDepartmentIgnoreCase(collegeId, department.trim());
        }
        return studentRepository.findByCollegeId(collegeId);
    }

    @Transactional
    public Student createStudent(
        Long collegeId,
        String regNo,
        String name,
        String address,
        String department,
        int year,
        LocalDate dob
    ){
        if(regNo == null || regNo.isBlank() || name == null || name.isBlank() || department == null || department.isBlank() || dob == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "regNo, name, department, and dob are required");
        }

        if(studentRepository.findByCollegeIdAndRegNoIgnoreCase(collegeId, regNo.trim()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student regNo already exists  in this college");
        }

        College college = collegeRepository.findById(collegeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        Student student = new Student();
        student.setCollege(college);
        student.setRegNo(regNo.trim().toUpperCase());
        student.setName(name.trim());
        student.setAddress(address);
        student.setDepartment(department.trim());
        student.setYear(year);
        student.setDob(dob);
        return studentRepository.save(student);
    }

    @Transactional
    public Student setActive(Long id, boolean active){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setActive(active);
        return studentRepository.save(student);
    }

    @Transactional
    public Student resetStudent(Long id){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setCollegeEmail(null);
        student.setFailedDobAttempts(0);
        student.setLockedUntil(null);
        return studentRepository.save(student);
    }
}