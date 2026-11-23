package com.bronzera.mybus.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>{
    Optional<Student> findByCollegeIdAndRegNoIgnoreCase(Long collegeId, String regNo);
    Optional<Student> findByCollegeEmailIgnoreCase(String collegeEmail);
    boolean existsByCollegeEmailIgnoreCase(String collegeEmail);
    List<Student> findByCollegeId(Long collegeId);
    List<Student> findByCollegeIdAndDepartmentIgnoreCase(Long collegeId, String department);
}