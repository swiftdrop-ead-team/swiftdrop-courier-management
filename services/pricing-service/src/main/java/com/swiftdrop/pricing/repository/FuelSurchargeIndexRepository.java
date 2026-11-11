package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FuelSurchargeIndexRepository extends JpaRepository<FuelSurchargeIndex, Long> {

    /**
     * Retrieves the most recent active fuel surcharge index whose effective date is on or before
     * the specified cutoff timestamp, ordered by effectiveFrom DESC, createdAt DESC.
     *
     * @param effectiveFrom the cutoff timestamp
     * @return the matching active fuel surcharge index, or empty if none found
     */
    Optional<FuelSurchargeIndex> findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(LocalDateTime effectiveFrom);

    /**
     * Retrieves the current active fuel surcharge index effective as of the specified timestamp.
     * Filters for active records with effectiveFrom <= now, ordered by effectiveFrom DESC, createdAt DESC.
     *
     * @param now the timestamp to evaluate effectiveness against
     * @return an Optional containing the matching FuelSurchargeIndex, or empty if none matches
     */
    default Optional<FuelSurchargeIndex> findCurrentActive(LocalDateTime now) {
        return findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(now);
    }

    /**
     * Retrieves the current active fuel surcharge index effective as of the current system time.
     *
     * @return an Optional containing the matching FuelSurchargeIndex, or empty if none matches
     */
    default Optional<FuelSurchargeIndex> findCurrentActive() {
        return findCurrentActive(LocalDateTime.now());
    }

    /**
     * Retrieves the latest active fuel surcharge index record using Spring Data query derivation.
     * Fully compliant with standard JPA without vendor-specific or invalid JPQL LIMIT syntax.
     */
    Optional<FuelSurchargeIndex> findFirstByIsActiveTrueOrderByEffectiveFromDescCreatedAtDesc();

    /**
     * Retrieves the latest active fuel surcharge index record without effective date cutoff.
     *
     * @deprecated Use {@link #findCurrentActive()} or {@link #findCurrentActive(LocalDateTime)} instead,
     *             which filter by effective date to prevent premature application of future rates.
     */
    @Deprecated
    default Optional<FuelSurchargeIndex> findLatestActive() {
        return findFirstByIsActiveTrueOrderByEffectiveFromDescCreatedAtDesc();
    }
}
