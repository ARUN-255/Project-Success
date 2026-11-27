package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;
import com.bronzera.mybus.route.Route;
import com.bronzera.mybus.route.RouteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService{

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final EntityManager entityManager;
    private final RouteRepository routeRepository;

    public StudentService(StudentRepository studentRepository, CollegeRepository collegeRepository, EntityManager entityManager, RouteRepository routeRepository){
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
        this.entityManager = entityManager;
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> listStudents(Long collegeId, String department, int page, int size){
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        Page<Student> students = (department != null && !department.isBlank())
            ? studentRepository.findByCollegeIdAndDepartmentIgnoreCase(collegeId, department.trim(), pageable)
            : studentRepository.findByCollegeId(collegeId, pageable);
        return students.map(StudentResponse::from);
}

    @Transactional
    public StudentResponse createStudent(
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
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse setActive(Long id, boolean active){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setActive(active);
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse resetStudent(Long id){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setCollegeEmail(null);
        student.setFailedDobAttempts(0);
        student.setLockedUntil(null);
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse setShareLocationAllowed(Long id, boolean allowed){
        Student student = studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setShareLocationAllowed(allowed);
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public void saveRoutePreference(Long studentId, Long routeId, Long stopId, String tripType){
        if(!"MORNING".equals(tripType) && !"EVENING".equals(tripType)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tripType must be MORNING or EVENING");
        }

        Student student = studentRepository.findById(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Route route = routeRepository.findById(routeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        if(!route.getBus().getCollege().getId().equals(student.getCollege().getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route belongs to a different college");
        }

        Number stopOnRoute = (Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM route_stop WHERE route_id = :rid AND stop_id = :stid")
            .setParameter("rid", routeId).setParameter("stid", stopId).getSingleResult();

            if(stopOnRoute.longValue() == 0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stop is not on this route");
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

    @Transactional(readOnly = true)
public List<RoutePrefResponse> getRoutePreferences(Long studentId){
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager.createNativeQuery(
        "SELECT trip_type, route_id, stop_id FROM student_route_pref WHERE student_id = :sid ORDER BY trip_type"
    ).setParameter("sid", studentId).getResultList();

    return rows.stream()
        .map(r -> new RoutePrefResponse(
            (String) r[0],
            ((Number) r[1]).longValue(),
            ((Number) r[2]).longValue()
        ))
        .toList();
}

public record StudentResponse(
    Long id,
    Long collegeId,
    String regNo,
    String name,
    String address,
    String department,
    int year,
    String collegeEmail,
    boolean shareLocationAllowed,
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
            s.getCollegeEmail(),
            s.isShareLocationAllowed(),
            s.isActive()
        );
    }
}

public record RoutePrefResponse(
    String tripType,
    Long routeId,
    Long stopId
) {}
}