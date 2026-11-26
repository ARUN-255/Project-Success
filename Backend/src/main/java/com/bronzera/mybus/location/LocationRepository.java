package com.bronzera.mybus.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<LocationPing, Long> {
    Optional<LocationPing> findTopByTripIdOrderByRecordedAtDesc(Long tripId);

    @Modifying
    @Query("DELETE FROM LocationPing p WHERE p.recordedAt < :cutoff")
    void deleteByRecordedAtBefore(OffsetDateTime cutoff);
}