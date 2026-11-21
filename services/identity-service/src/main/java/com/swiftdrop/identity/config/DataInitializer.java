package com.swiftdrop.identity.config;

import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.repository.MerchantApprovalRecordRepository;
import com.swiftdrop.identity.repository.RoleRepository;
import com.swiftdrop.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MerchantApprovalRecordRepository merchantApprovalRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running SwiftDrop Identity Data Initializer...");

        // Ensure roles exist
        Role adminRole = getOrCreateRole(RoleName.ROLE_ADMIN, "Platform Administrator with full access");
        Role merchantRole = getOrCreateRole(RoleName.ROLE_MERCHANT, "Commercial merchant with parcel dispatch capabilities");
        Role courierRole = getOrCreateRole(RoleName.ROLE_COURIER, "Field courier with delivery management capabilities");
        Role customerRole = getOrCreateRole(RoleName.ROLE_CUSTOMER, "Standard individual retail consumer");

        // Seed 1: Platform Administrator
        if (!userRepository.existsByEmailIgnoreCase("admin@swiftdrop.io")) {
            User admin = User.builder()
                    .email("admin@swiftdrop.io")
                    .fullName("SwiftDrop Administrator")
                    .phoneNumber("+18005550100")
                    .passwordHash(passwordEncoder.encode("Admin@SwiftDrop2026!"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Seeded administrator account: admin@swiftdrop.io");
        }

        // Seed 2: Customer Account
        if (!userRepository.existsByEmailIgnoreCase("customer@swiftdrop.io")) {
            User customer = User.builder()
                    .email("customer@swiftdrop.io")
                    .fullName("Alex Morgan")
                    .phoneNumber("+18005550101")
                    .passwordHash(passwordEncoder.encode("Customer@123!"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(customerRole))
                    .build();
            userRepository.save(customer);
            log.info("Seeded customer account: customer@swiftdrop.io");
        }

        // Seed 3: Pending Merchant
        if (!userRepository.existsByEmailIgnoreCase("merchant.pending@swiftdrop.io")) {
            User merchant = User.builder()
                    .email("merchant.pending@swiftdrop.io")
                    .fullName("Marcus Vance")
                    .phoneNumber("+18005550102")
                    .passwordHash(passwordEncoder.encode("Merchant@123!"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(merchantRole))
                    .build();
            merchant = userRepository.save(merchant);

            MerchantApprovalRecord pendingRecord = MerchantApprovalRecord.builder()
                    .user(merchant)
                    .businessName("Nexus Logistics Global")
                    .contactPerson("Marcus Vance")
                    .businessAddress("789 Commercial Blvd, Suite 400, Chicago, IL")
                    .phoneNumber("+18005550102")
                    .status(MerchantApprovalStatus.PENDING)
                    .notes("Initial business documentation uploaded, awaiting verification")
                    .build();
            merchantApprovalRecordRepository.save(pendingRecord);
            log.info("Seeded pending merchant account: merchant.pending@swiftdrop.io");
        }

        // Seed 4: Approved Merchant
        if (!userRepository.existsByEmailIgnoreCase("merchant.approved@swiftdrop.io")) {
            User merchant = User.builder()
                    .email("merchant.approved@swiftdrop.io")
                    .fullName("Sarah Jenkins")
                    .phoneNumber("+18005550103")
                    .passwordHash(passwordEncoder.encode("Merchant@123!"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(merchantRole))
                    .build();
            merchant = userRepository.save(merchant);

            MerchantApprovalRecord approvedRecord = MerchantApprovalRecord.builder()
                    .user(merchant)
                    .businessName("Pacific Express Goods")
                    .contactPerson("Sarah Jenkins")
                    .businessAddress("101 Market Wharf, Seattle, WA")
                    .phoneNumber("+18005550103")
                    .status(MerchantApprovalStatus.APPROVED)
                    .notes("Verified with Secretary of State commercial registry")
                    .build();
            merchantApprovalRecordRepository.save(approvedRecord);
            log.info("Seeded approved merchant account: merchant.approved@swiftdrop.io");
        }

        // Seed 5: Field Courier
        if (!userRepository.existsByEmailIgnoreCase("courier@swiftdrop.io")) {
            User courier = User.builder()
                    .email("courier@swiftdrop.io")
                    .fullName("David Chen")
                    .phoneNumber("+18005550104")
                    .passwordHash(passwordEncoder.encode("Courier@123!"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(courierRole))
                    .build();
            userRepository.save(courier);
            log.info("Seeded courier account: courier@swiftdrop.io");
        }
    }

    private Role getOrCreateRole(RoleName roleName, String description) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleName)
                        .description(description)
                        .build()));
    }
}
