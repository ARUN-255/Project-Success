package com.bronzera.mybus.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>{
    Optional<Student> findByCollegeIdAndRegNoIgnoreCase(Long collegeId, String regNo);
    Optional<Student> findByCollegeEmailIgnoreCase(String collegeEmail);
    boolean existsByCollegeEmailIgnoreCase(String collegeEmail);
    List<Student> findByCollegeId(Long collegeId);
    List<Student> findByCollegeIdAndDepartmentIgnoreCase(Long collegeId, String department);
    Page<Student> findByCollegeId(Long collegeId, Pageable pageable);
    Page<Student> findByCollegeIdAndDepartmentIgnoreCase(Long collegeId, String department, Pageable pageable);
}