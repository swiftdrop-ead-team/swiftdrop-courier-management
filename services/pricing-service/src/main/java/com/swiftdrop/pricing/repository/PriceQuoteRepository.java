package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, UUID> {
    Optional<PriceQuote> findByIdAndExpiresAtAfter(UUID id, LocalDateTime now);
    List<PriceQuote> findByExpiresAtBefore(LocalDateTime cutoff);
}
