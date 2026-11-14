package com.swiftdrop.identity.repository;

import com.swiftdrop.identity.entity.Role;
import com.swiftdrop.identity.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}
