package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.Courier;
import com.swiftdrop.dispatch.entity.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Courier entity.
 * Provides automated database query execution on the "couriers" table.
 */
@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    // Finds a courier profile by their Identity Service user ID
    Optional<Courier> findByUserId(String userId);

    // Finds all couriers matching a specific operational status (e.g., AVAILABLE)
    List<Courier> findByStatus(CourierStatus status);

    // Finds couriers located in a specific delivery zone with a given status
    List<Courier> findByAssignedZoneAndStatus(String assignedZone, CourierStatus status);

    /**
     * Custom JPQL query: Finds all couriers who are AVAILABLE and have not exceeded
     * their maxActiveDeliveries limit, ordered by lowest current active load first
     * (Greedy load-balancing strategy).
     */
    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' AND c.currentActiveDeliveries < c.maxActiveDeliveries ORDER BY c.currentActiveDeliveries ASC")
    List<Courier> findAvailableCouriersWithCapacity();

    /**
     * Custom JPQL query: Finds available couriers with capacity filtered by geographical zone.
     */
    @Query("SELECT c FROM Courier c WHERE c.assignedZone = :zone AND c.status = 'AVAILABLE' AND c.currentActiveDeliveries < c.maxActiveDeliveries ORDER BY c.currentActiveDeliveries ASC")
    List<Courier> findAvailableCouriersByZoneWithCapacity(@Param("zone") String zone);
}
