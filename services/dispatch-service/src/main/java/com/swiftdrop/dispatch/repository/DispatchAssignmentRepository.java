package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import com.swiftdrop.dispatch.entity.DispatchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    List<DispatchAssignment> findByCourierId(Long courierId);

    List<DispatchAssignment> findByCourierIdAndStatusIn(Long courierId, List<AssignmentStatus> statuses);

    Optional<DispatchAssignment> findByParcelId(String parcelId);

    Optional<DispatchAssignment> findByTrackingNumber(String trackingNumber);

    List<DispatchAssignment> findByStatus(AssignmentStatus status);

    long countByCourierIdAndStatusIn(Long courierId, List<AssignmentStatus> statuses);
}

