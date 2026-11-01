package com.swiftdrop.identity.entity;

public enum RoleName {
    ROLE_CUSTOMER,
    ROLE_MERCHANT,
    ROLE_COURIER,
    ROLE_ADMIN;

    public String getCleanName() {
        return name().replace("ROLE_", "");
    }
}
