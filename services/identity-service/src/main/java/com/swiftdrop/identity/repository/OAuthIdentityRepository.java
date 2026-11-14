package com.swiftdrop.identity.repository;

import com.swiftdrop.identity.entity.OAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {
    Optional<OAuthIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OAuthIdentity> findByUserIdAndProvider(UUID userId, String provider);
}
