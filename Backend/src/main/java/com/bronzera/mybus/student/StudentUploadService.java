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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentUploadService {

    private static final int MAX_ROWS = 5000;

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final Clock clock;

    public StudentUploadService(
        StudentRepository studentRepository,
        CollegeRepository collegeRepository,
        Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
        this.clock = clock;
    }

    @Transactional
    public UploadResult processCsv(Long collegeId, MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
        }

        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        List<String> errors = new ArrayList<>();
        List<StudentRow> validRows = new ArrayList<>();
        Set<String> seenRegNos = new HashSet<>();
        LocalDate today = LocalDate.now(clock);

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
                Map<String, Integer> headers = parser.getHeaderMap();
                if (headers == null || !headers.containsKey("reg_no") || !headers.containsKey("name")
                        || !headers.containsKey("department") || !headers.containsKey("year") || !headers.containsKey("dob")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "CSV header must include reg_no, name, address, department, year, dob");
                }

                for (CSVRecord record : parser) {
                    long rowNum = record.getRecordNumber();
                    if (rowNum > MAX_ROWS) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "CSV exceeds maximum of " + MAX_ROWS + " rows");
                    }

                    String regNo = record.get("reg_no").trim().toUpperCase();
                    String name = record.get("name").trim();
                    String rawAddress = record.isMapped("address") ? record.get("address").trim() : "";
                    String address = rawAddress.isEmpty() ? null : rawAddress;
                    String department = record.get("department").trim();
                    String yearStr = record.get("year").trim();
                    String dobStr = record.get("dob").trim();

                    if (regNo.isEmpty() || name.isEmpty() || department.isEmpty()) {
                        errors.add("Row " + rowNum + ": reg_no, name, and department are required");
                        continue;
                    }

                    if (!seenRegNos.add(regNo)) {
                        errors.add("Row " + rowNum + ": duplicate reg_no in CSV (" + regNo + ")");
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
                    if (dob.isAfter(today)) {
                        errors.add("Row " + rowNum + ": dob cannot be in the future");
                        continue;
                    }

                    validRows.add(new StudentRow(regNo, name, address, department, year, dob));
                }
            }
        } catch (IOException | IllegalArgumentException | UncheckedIOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV file: " + e.getMessage());
        }

        if (!dryRun && errors.isEmpty()) {
            for (StudentRow row : validRows) {
                Student student = studentRepository
                        .findByCollegeIdAndRegNoIgnoreCase(collegeId, row.regNo())
                        .orElseGet(Student::new);
                student.setCollege(college);
                student.setRegNo(row.regNo());
                student.setName(row.name());
                student.setAddress(row.address());
                student.setDepartment(row.department());
                student.setYear(row.year());
                student.setDob(row.dob());
                studentRepository.save(student);
            }
        }

        return new UploadResult(validRows.size(), errors.size(), errors);
    }

    private record StudentRow(String regNo, String name, String address, String department, int year, LocalDate dob) {}

    public record UploadResult(int validCount, int errorCount, List<String> errors) {}
}