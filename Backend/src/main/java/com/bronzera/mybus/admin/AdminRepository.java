package com.bronzera.mybus.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long>{
    Optional<Admin> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Admin> findByCollegeId(Long collegeId);
    List<Admin> findByOwnerAccessRequestedTrue();
}