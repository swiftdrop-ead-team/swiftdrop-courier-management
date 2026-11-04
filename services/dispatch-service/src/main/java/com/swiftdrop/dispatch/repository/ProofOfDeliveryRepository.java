package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for ProofOfDelivery entity.
 * Handles database operations on the "proof_of_delivery" table.
 */
@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    // Retrieves the proof of delivery record for a specific dispatch assignment ID
    Optional<ProofOfDelivery> findByAssignmentId(Long assignmentId);
}
