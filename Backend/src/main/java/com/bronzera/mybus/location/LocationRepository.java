package com.bronzera.mybus.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<LocationPing, Long> {
    Optional<LocationPing> findTopByTripIdOrderByRecordedAtDesc(Long tripId);
    void deleteByRecordedAtBefore(OffsetDateTime cutoff);
}