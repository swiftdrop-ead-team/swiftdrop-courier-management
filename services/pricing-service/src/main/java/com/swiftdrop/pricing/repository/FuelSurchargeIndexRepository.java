package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelSurchargeIndexRepository extends JpaRepository<FuelSurchargeIndex, Long> {

    /**
     * Retrieves the latest active fuel surcharge index record using Spring Data query derivation.
     * Fully compliant with standard JPA without vendor-specific or invalid JPQL LIMIT syntax.
     */
    Optional<FuelSurchargeIndex> findFirstByIsActiveTrueOrderByEffectiveFromDescCreatedAtDesc();

    default Optional<FuelSurchargeIndex> findLatestActive() {
        return findFirstByIsActiveTrueOrderByEffectiveFromDescCreatedAtDesc();
    }
}
