package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByServiceType(ServiceType serviceType);
    Optional<PricingRule> findByServiceTypeAndIsActiveTrue(ServiceType serviceType);
    List<PricingRule> findByIsActiveTrue();
}
