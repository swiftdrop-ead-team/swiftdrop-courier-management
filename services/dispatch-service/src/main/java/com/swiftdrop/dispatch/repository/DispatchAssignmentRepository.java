package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import com.swiftdrop.dispatch.entity.DispatchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for DispatchAssignment entity.
 * Provides database operations on the "dispatch_assignments" table.
 */
public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    // Retrieves all assignments given to a specific courier
    List<DispatchAssignment> findByCourierId(Long courierId);

    // Retrieves assignments for a courier filtered by a list of statuses (e.g. active assignments)
    List<DispatchAssignment> findByCourierIdAndStatusIn(Long courierId, List<AssignmentStatus> statuses);

    // Finds an assignment by the parcel's unique ID
    Optional<DispatchAssignment> findByParcelId(String parcelId);

    // Finds an assignment using the public tracking number (e.g., SWIFT-COL-1029)
    Optional<DispatchAssignment> findByTrackingNumber(String trackingNumber);

    // Finds all assignments in a specific lifecycle status
    List<DispatchAssignment> findByStatus(AssignmentStatus status);

    // Counts how many active deliveries a courier currently holds
    long countByCourierIdAndStatusIn(Long courierId, List<AssignmentStatus> statuses);
}
