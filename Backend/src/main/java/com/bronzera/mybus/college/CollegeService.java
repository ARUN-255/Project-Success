package com.bronzera.mybus.college;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CollegeService{

    private final CollegeRepository collegeRepository;

    public CollegeService(CollegeRepository collegeRepository){
        this.collegeRepository = collegeRepository;
    }

    @Transactional(readOnly = true)
    public List<CollegeResponse> listColleges(){
        return collegeRepository.findAll().stream().map(CollegeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CollegeResponse getCollege(Long id){
        College college = collegeRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));
        return CollegeResponse.from(college);
    }

    @Transactional
    public CollegeResponse createCollege(String name, String emailDomain){
        String cleanName = name.trim();
        String domain = emailDomain.trim().toLowerCase();
        if(domain.startsWith("@")){
            domain = domain.substring(1);
        }

        if(collegeRepository.existsByNameIgnoreCase(cleanName)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "College already exists");
        }

        College college = new College();
        college.setName(cleanName);
        college.setEmailDomain(domain);
        return CollegeResponse.from(collegeRepository.save(college));
    }

    public record CollegeResponse(Long id, String name, String emailDomain){
        public static CollegeResponse from(College c){
            return new CollegeResponse(c.getId(), c.getName(), c.getEmailDomain());
        }
    }
}
