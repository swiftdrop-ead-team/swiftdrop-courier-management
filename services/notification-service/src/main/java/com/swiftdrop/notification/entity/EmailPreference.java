package com.swiftdrop.notification.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="email_preferences") @Getter @Setter @NoArgsConstructor
public class EmailPreference {
 @Id @Column(length=255) private String email;
 @Column(nullable=false) private boolean enabled=true;
}
