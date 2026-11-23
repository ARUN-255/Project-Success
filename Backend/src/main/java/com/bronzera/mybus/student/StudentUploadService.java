package com.bronzera.mybus.student;

import com.bronzera.mybus.college.College;
import com.bronzera.mybus.college.CollegeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public  class StudentUploadService{

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;

    public StudentUploadService(StudentRepository studentRepository, CollegeRepository collegeRepository){
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
    }

    @Transactional
    public UploadResult processCsv(Long collegeId, MultipartFile file, boolean dryRun){
        College college = collegeRepository.findById(collegeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        List<String> errors = new ArrayList<>();
        int validCount = 0;

        try(BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )){
            String line;
            int rowNum = 0;
            while ((line = reader.readLine()) != null){
                rowNum++;
                if(line.isBlank()) continue;
                if(rowNum == 1 && line.toLowerCase().contains("reg_no")) continue;

                String[] cols = line.split(",", -1);
                if(cols.length<6){
                    errors.add("Row" + rowNum + ": expected 6 columns (reg_no, name, address, department, year, dob");
                    continue;
                }
                String regNo = cols[0].trim().toUpperCase();
                String name = cols[1].trim();
                String address = cols[2].trim();
                String department = cols[3].trim();
                String yearStr = cols[4].trim();
                String dobStr = cols[5].trim();

                if(regNo.isEmpty() || name.isEmpty() || department.isEmpty()){
                    errors.add("Row " + rowNum + ": reg_no, name, and department are required");
                    continue;
                }
                
                int year;
                LocalDate dob;
                try{
                    year = Integer.parseInt(yearStr);
                    dob = LocalDate.parse(dobStr);
                }catch (Exception ex){
                    errors.add("Row " + rowNum + ": invalid year or dob (use YYYY-MM-DD");
                    continue;
                }

                validCount++;
                if(!dryRun){
                    Student student = studentRepository.findByCollegeIdAndRegNoIgnoreCase(collegeId, regNo).orElseGet(Student::new);
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
            
            }catch (Exception e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file");
        }
        return new UploadResult(validCount, errors.size(), errors);
    }
    public record UploadResult(int validCount, int errorCount, List<String> errors) {}
}