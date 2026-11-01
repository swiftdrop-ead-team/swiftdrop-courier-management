package com.swiftdrop.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "oauth_identities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_oauth_provider_user", columnNames = {"provider", "provider_user_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 150)
    private String providerUserId;

    @Column(name = "provider_email", length = 150)
    private String providerEmail;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;
}
