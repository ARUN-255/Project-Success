package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService{

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final EntityManager entityManager;

    public StudentService(StudentRepository studentRepository, CollegeRepository collegeRepository, EntityManager entityManager){
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
        this.entityManager = entityManager;
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

    @Transactional
    public Student setShareLocationAllowed(Long id, boolean allowed){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setShareLocationAllowed(allowed);
        return studentRepository.save(student);
    }

    @Transactional
    public void saveRoutePreference(Long studentId, Long routeId, Long stopId, String tripType){
        if(!"MORNING".equals(tripType) && !"EVENING".equals(tripType)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tripType must be MORNING or EVENING");
        }
        entityManager.createNativeQuery(
            "INSERT INTO student_route_pref (student_id, route_id, stop_id, trip_type, updated_at) "
            + "VALUES (:sid, :rid, :stid, :tt, NOW()) "
            + "ON CONFLICT (student_id, trip_type) DO UPDATE "
            + "SET route_id = EXCLUDED.route_id, stop_id = EXCLUDED.stop_id, updated_at = NOW() "
        )
        .setParameter("sid", studentId)
        .setParameter("rid", routeId)
        .setParameter("stid", stopId)
        .setParameter("tt", tripType)
        .executeUpdate();
    }
}