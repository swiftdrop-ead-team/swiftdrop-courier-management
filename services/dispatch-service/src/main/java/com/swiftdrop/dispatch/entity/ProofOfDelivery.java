package com.swiftdrop.dispatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ProofOfDelivery (POD) entity holding delivery evidence and confirmation data.
 * Mapped to the "proof_of_delivery" table in PostgreSQL.
 */
@Entity
@Table(name = "proof_of_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfDelivery {

    // Unique primary key ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key link to the associated dispatch assignment
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private DispatchAssignment assignment;

    // Name of the individual who physically received the parcel
    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    // National Identity Card (NIC) or ID number of recipient (optional verification)
    @Column(name = "recipient_nic", length = 50)
    private String recipientNic;

    // URL to electronic digital signature image stored on cloud / storage
    @Column(name = "signature_url", columnDefinition = "TEXT")
    private String signatureUrl;

    // URL to photographed proof of package delivery at customer doorstep
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    // Courier's delivery confirmation notes (e.g., "Left at reception desk")
    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    // Timestamp when package was delivered and POD was registered
    @Column(name = "delivered_at", nullable = false)
    @Builder.Default
    private LocalDateTime deliveredAt = LocalDateTime.now();
}
