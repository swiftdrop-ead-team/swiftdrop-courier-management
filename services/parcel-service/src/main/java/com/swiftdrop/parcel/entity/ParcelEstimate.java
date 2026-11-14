package com.swiftdrop.parcel.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="parcel_estimates") @Getter @Setter @NoArgsConstructor
public class ParcelEstimate {
 @Id private UUID id;
 @Column(nullable=false) private UUID ownerId;
 @Column(nullable=false,columnDefinition="TEXT") private String payload;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=10) private String currency;
 @Column(nullable=false) private Instant expiresAt;
 private Instant consumedAt;
}
