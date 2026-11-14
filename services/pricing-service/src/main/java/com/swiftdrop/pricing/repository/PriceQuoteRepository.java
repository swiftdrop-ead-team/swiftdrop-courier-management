package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceQuoteRepository extends JpaRepository<PriceQuote, UUID> {
    Optional<PriceQuote> findByIdAndExpiresAtAfter(UUID id, LocalDateTime now);
    List<PriceQuote> findByExpiresAtBefore(LocalDateTime cutoff);
}
