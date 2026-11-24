package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentUploadService {

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;

    public StudentUploadService(StudentRepository studentRepository, CollegeRepository collegeRepository) {
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
    }

    @Transactional
    public UploadResult processCsv(Long collegeId, MultipartFile file, boolean dryRun) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        List<String> errors = new ArrayList<>();
        int validCount = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.mark(1);
            if (reader.read() != 0xFEFF) {
                reader.reset();
            }
            try (CSVParser parser = format.parse(reader)) {
                for (CSVRecord record : parser) {
                    long rowNum = record.getRecordNumber();
                    if (!record.isMapped("reg_no") || !record.isMapped("name")
                            || !record.isMapped("department") || !record.isMapped("year") || !record.isMapped("dob")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "CSV header must include reg_no, name, address, department, year, dob");
                    }

                    String regNo = record.get("reg_no").trim().toUpperCase();
                    String name = record.get("name").trim();
                    String address = record.isMapped("address") ? record.get("address").trim() : "";
                    String department = record.get("department").trim();
                    String yearStr = record.get("year").trim();
                    String dobStr = record.get("dob").trim();

                    if (regNo.isEmpty() || name.isEmpty() || department.isEmpty()) {
                        errors.add("Row " + rowNum + ": reg_no, name, and department are required");
                        continue;
                    }

                    int year;
                    LocalDate dob;
                    try {
                        year = Integer.parseInt(yearStr);
                        dob = LocalDate.parse(dobStr);
                    } catch (Exception ex) {
                        errors.add("Row " + rowNum + ": invalid year or dob (use YYYY-MM-DD)");
                        continue;
                    }

                    if (year < 1 || year > 4) {
                        errors.add("Row " + rowNum + ": year must be between 1 and 4");
                        continue;
                    }
                    if (dob.isAfter(LocalDate.now())) {
                        errors.add("Row " + rowNum + ": dob cannot be in the future");
                        continue;
                    }

                    validCount++;
                    if (!dryRun) {
                        Student student = studentRepository
                                .findByCollegeIdAndRegNoIgnoreCase(collegeId, regNo)
                                .orElseGet(Student::new);
                        student.setCollege(college);
                        student.setRegNo(regNo);
                        student.setName(name);
                        student.setAddress(address);
                        student.setDepartment(department);
                        student.setYear(year);
                        student.setDob(dob);
                        studentRepository.save(student);
                    }
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file");
        }

        return new UploadResult(validCount, errors.size(), errors);
    }

    public record UploadResult(int validCount, int errorCount, List<String> errors) {}
}