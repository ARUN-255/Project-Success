package com.bronzera.mybus.college;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public List<CollegeService.CollegeResponse> listColleges() {
        return collegeService.listColleges();
    }

    @GetMapping("/{id}")
    public CollegeService.CollegeResponse getCollege(@PathVariable Long id) {
        return collegeService.getCollege(id);
    }

    @PostMapping
    public ResponseEntity<CollegeService.CollegeResponse> createCollege(@Valid @RequestBody CreateCollegeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collegeService.createCollege(request.name(), request.emailDomain()));
    }

    public record CreateCollegeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Use a bare domain like gct.ac.in") String emailDomain
    ) {}
}