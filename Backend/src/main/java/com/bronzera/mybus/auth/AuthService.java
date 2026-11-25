package com.bronzera.mybus.auth;

import com.bronzera.mybus.admin.Admin;
import com.bronzera.mybus.admin.AdminRepository;
import com.bronzera.mybus.driver.Driver;
import com.bronzera.mybus.driver.DriverRepository;
import com.bronzera.mybus.student.Student;
import com.bronzera.mybus.student.StudentRepository;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.time.Clock;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final DriverRepository driverRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final EntityManager entityManager;
    private final StringRedisTemplate redisTemplate;
    private final String mailUsername;
    private final long otpExpirationMinutes;
    private final Environment env;
    private final Clock clock;

    public AuthService(
            DriverRepository driverRepository,
            StudentRepository studentRepository,
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JavaMailSender mailSender,
            EntityManager entityManager,
            StringRedisTemplate redisTemplate,
            Environment env,
            Clock clock,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${app.otp.expiration-minutes:10}") long otpExpirationMinutes) {
        this.driverRepository = driverRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.entityManager = entityManager;
        this.redisTemplate = redisTemplate;
        this.mailUsername = mailUsername;
        this.otpExpirationMinutes = otpExpirationMinutes;
        this.env = env;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public DriverLoginResponse driverLogin(String loginId, String password, String deviceId) {
        Driver driver = driverRepository.findByLoginIdIgnoreCase(loginId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ID not registered. Contact the transport office"));

        if (!driver.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has been disabled by Admin. Contact your admin");
        }
        if (driver.getLockedUntil() != null && driver.getLockedUntil().isAfter(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Account locked. Try again later");
        }
        if (!passwordEncoder.matches(password, driver.getPasswordHash())) {
            int attempts = driver.getFailedAttempts() + 1;
            driver.setFailedAttempts(attempts);
            if (attempts >= 5) {
                driver.setLockedUntil(OffsetDateTime.now(clock).plusMinutes(15));
            }
            driverRepository.save(driver);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        driver.setFailedAttempts(0);
        driver.setLockedUntil(null);
        if (deviceId != null && !deviceId.isBlank()) {
            driver.setActiveDeviceId(deviceId.trim());
        }
        driverRepository.save(driver);

        String token = jwtService.generateToken(driver.getId(), "DRIVER", driver.getCollege().getId());
        return new DriverLoginResponse(token, driver.getId(), driver.getName(), driver.isMustChangePassword());
    }

    @Transactional
    public void driverChangePassword(Long driverId, LocalDate dob, String oldPassword, String newPassword) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        if (!driver.getDob().equals(dob) || !passwordEncoder.matches(oldPassword, driver.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOB or current password is incorrect");
        }
        driver.setPasswordHash(passwordEncoder.encode(newPassword));
        driver.setMustChangePassword(false);
        driverRepository.save(driver);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AdminLoginResponse adminLogin(String email, String password) {
        Admin admin = adminRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!admin.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        }
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Account locked. Try again later");
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            int attempts = admin.getFailedAttempts() + 1;
            admin.setFailedAttempts(attempts);
            if (attempts >= 5) {
                admin.setLockedUntil(OffsetDateTime.now(clock).plusMinutes(15));
            }
            adminRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        admin.setFailedAttempts(0);
        admin.setLockedUntil(null);
        adminRepository.save(admin);

        String role = admin.isOwner() ? "OWNER" : "ADMIN";
        Long collegeId = admin.getCollege() != null ? admin.getCollege().getId() : null;
        String token = jwtService.generateToken(admin.getId(), role, collegeId);
        return new AdminLoginResponse(token, admin.getId(), role, collegeId);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public StudentPreviewResponse studentLookup(Long collegeId, String regNo, LocalDate dob) {
        Student student = studentRepository.findByCollegeIdAndRegNoIgnoreCase(collegeId, regNo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (!student.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account is inactive");
        }
        if (student.getLockedUntil() != null && student.getLockedUntil().isAfter(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong attempts. Try again later");
        }
        if (!student.getDob().equals(dob)) {
            int attempts = student.getFailedDobAttempts() + 1;
            student.setFailedDobAttempts(attempts);
            if (attempts >= 5) {
                student.setLockedUntil(OffsetDateTime.now(clock).plusMinutes(15));
            }
            studentRepository.save(student);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth does not match");
        }

        student.setFailedDobAttempts(0);
        student.setLockedUntil(null);
        studentRepository.save(student);

        String lookupToken = jwtService.generateLookupToken(student.getId());
        return new StudentPreviewResponse(
                lookupToken,
                student.getId(),
                student.getRegNo(),
                student.getName(),
                student.getAddress(),
                student.getCollege().getEmailDomain()
        );
    }

    @Transactional
    public void sendStudentOtp(String lookupToken, String email) {
        Long studentId = extractLookupStudentId(lookupToken);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (!student.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account is inactive");
        }

        String cleanEmail = email.trim().toLowerCase();
        if (student.getCollegeEmail() != null && !student.getCollegeEmail().equalsIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already verified with a different email");
        }

        String domain = "@" + student.getCollege().getEmailDomain().toLowerCase();
        if (!cleanEmail.endsWith(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only college email addresses are allowed");
        }
        studentRepository.findByCollegeEmailIgnoreCase(cleanEmail).ifPresent(existing -> {
            if (!existing.getId().equals(student.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already linked to another student");
            }
        });

        String cooldownKey = "otp:cooldown:" + cleanEmail;
        Boolean firstSend = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(30));
        if (Boolean.FALSE.equals(firstSend)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait 30 seconds before requesting another code");
        }

        String hourlyKey = "otp:hourly:" + cleanEmail;
        Long count = redisTemplate.opsForValue().increment(hourlyKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(hourlyKey, Duration.ofHours(1));
        }
        if (count != null && count > 5L) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Hourly OTP limit reached. Try again later");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).plusMinutes(otpExpirationMinutes);

        entityManager.createNativeQuery(
                        "INSERT INTO email_otp (email, code_hash, expires_at) VALUES (:email, :hash, :exp)")
                .setParameter("email", cleanEmail)
                .setParameter("hash", codeHash)
                .setParameter("exp", expiresAt)
                .executeUpdate();

        if (mailUsername != null && !mailUsername.isBlank()) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(cleanEmail);
            msg.setSubject("My College Bus - Verification Code");
            msg.setText("Your 6-digit login code is: " + code + " (valid for " + otpExpirationMinutes + " minutes).");
            mailSender.send(msg);
        } else if (env.acceptsProfiles(Profiles.of("local"))){
            log.info("[LOCAL OTP] {} -> {}", cleanEmail, code);
        } else {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured");
        }
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public StudentLoginResponse verifyStudentOtp(String lookupToken, String email, String code) {
        Long studentId = extractLookupStudentId(lookupToken);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (!student.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account is inactive");
        }

        String cleanEmail = email.trim().toLowerCase();
        if (student.getCollegeEmail() != null && !student.getCollegeEmail().equalsIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already verified with a different email");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id, code_hash, expires_at, attempts FROM email_otp WHERE email = :email ORDER BY created_at DESC LIMIT 1")
                .setParameter("email", cleanEmail)
                .getResultList();

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No OTP found for this email");
        }

        Object[] row = rows.get(0);
        Long otpId = ((Number) row[0]).longValue();
        String codeHash = (String) row[1];
        OffsetDateTime expiresAt = toOffsetDateTime(row[2]);
        int attempts = ((Number) row[3]).intValue();

        if (attempts >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed OTP attempts");
        }
        if (expiresAt.isBefore(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }
        if (!passwordEncoder.matches(code.trim(), codeHash)) {
            entityManager.createNativeQuery("UPDATE email_otp SET attempts = attempts + 1 WHERE id = :id")
                    .setParameter("id", otpId)
                    .executeUpdate();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        entityManager.createNativeQuery("DELETE FROM email_otp WHERE id = :id")
                .setParameter("id", otpId)
                .executeUpdate();

        student.setCollegeEmail(cleanEmail);
        studentRepository.save(student);

        String token = jwtService.generateToken(student.getId(), "STUDENT", student.getCollege().getId());
        return new StudentLoginResponse(token, student.getId(), student.getName(), cleanEmail);
    }

    private Long extractLookupStudentId(String lookupToken) {
        if (lookupToken == null || lookupToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "lookupToken is required");
        }
        try {
            Claims claims = jwtService.parseToken(lookupToken);
            if (!"STUDENT_LOOKUP".equals(claims.get("role", String.class))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid lookupToken");
            }
            return Long.parseLong(claims.getSubject());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired lookupToken");
        }
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof Instant inst) return inst.atOffset(ZoneOffset.UTC);
        if (value instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("Unexpected timestamp type: " + value.getClass());
    }

    public record DriverLoginResponse(String token, Long driverId, String name, boolean mustChangePassword) {}
    public record AdminLoginResponse(String token, Long adminId, String role, Long collegeId) {}
    public record StudentPreviewResponse(String lookupToken, Long studentId, String regNo, String name, String address, String emailDomain) {}
    public record StudentLoginResponse(String token, Long studentId, String name, String email) {}
}