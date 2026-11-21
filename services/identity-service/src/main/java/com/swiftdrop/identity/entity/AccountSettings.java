package com.swiftdrop.identity.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name="account_settings") @Getter @Setter @NoArgsConstructor
public class AccountSettings {
 @Id private UUID userId;
 @Column(length=500) private String address="";
 @Column(nullable=false) private boolean emailNotifications=true;
 @Column(nullable=false,columnDefinition="TEXT") private String addressesJson="[]";
}
