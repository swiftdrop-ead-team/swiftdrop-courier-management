package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    Optional<ProofOfDelivery> findByAssignmentId(Long assignmentId);
}

