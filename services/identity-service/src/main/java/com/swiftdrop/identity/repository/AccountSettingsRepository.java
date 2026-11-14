package com.swiftdrop.identity.repository;
import com.swiftdrop.identity.entity.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AccountSettingsRepository extends JpaRepository<AccountSettings,UUID> {}
