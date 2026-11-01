package com.swiftdrop.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_approval_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "contact_person", nullable = false, length = 150)
    private String contactPerson;

    @Column(name = "business_address", nullable = false, columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MerchantApprovalStatus status = MerchantApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
