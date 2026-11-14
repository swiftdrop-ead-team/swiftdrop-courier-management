package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.PromoCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);
    Optional<PromoCode> findByCodeAndIsActiveTrue(String code);
    List<PromoCode> findByCodeIgnoreCase(String code);

    /**
     * Finds promo code by code string and acquires a pessimistic write lock to guarantee
     * concurrency protection against race conditions during redemption checks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCode p WHERE p.code = :code")
    Optional<PromoCode> findByCodeWithPessimisticLock(@Param("code") String code);
}
