package com.swiftdrop.notification.repository;
import com.swiftdrop.notification.entity.EmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EmailPreferenceRepository extends JpaRepository<EmailPreference,String> {}
