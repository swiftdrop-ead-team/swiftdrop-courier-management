package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
    Optional<DeliveryZone> findByZoneCode(String zoneCode);
    Optional<DeliveryZone> findByZoneCodeIgnoreCase(String zoneCode);
    List<DeliveryZone> findByIsActiveTrue();
    boolean existsByZoneCode(String zoneCode);
}
