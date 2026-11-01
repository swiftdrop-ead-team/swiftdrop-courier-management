package com.swiftdrop.identity.repository;

import com.swiftdrop.identity.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshSession r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
