package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.entity.Courier;
import com.swiftdrop.dispatch.entity.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    Optional<Courier> findByUserId(String userId);

    List<Courier> findByStatus(CourierStatus status);

    List<Courier> findByAssignedZoneAndStatus(String assignedZone, CourierStatus status);

    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' AND c.currentActiveDeliveries < c.maxActiveDeliveries ORDER BY c.currentActiveDeliveries ASC")
    List<Courier> findAvailableCouriersWithCapacity();

    @Query("SELECT c FROM Courier c WHERE c.assignedZone = :zone AND c.status = 'AVAILABLE' AND c.currentActiveDeliveries < c.maxActiveDeliveries ORDER BY c.currentActiveDeliveries ASC")
    List<Courier> findAvailableCouriersByZoneWithCapacity(@Param("zone") String zone);
}

