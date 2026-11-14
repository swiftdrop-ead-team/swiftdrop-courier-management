package com.swiftdrop.parcel.repository;

import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.entity.ParcelStatusHistory;
import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ParcelRepositoryTest {

    @Autowired
    private ParcelRepository parcelRepository;

    @Test
    @DisplayName("Should persist a parcel and find it by tracking number")
    void shouldPersistAndFindByTrackingNumber() {
        UUID senderId = UUID.randomUUID();
        Parcel parcel = Parcel.builder()
                .trackingNumber("SD-7777-8888-LK")
                .senderId(senderId)
                .senderType(SenderType.CUSTOMER)
                .senderName("Jane Doe")
                .senderPhone("+94770001122")
                .pickupAddress("10 Main Street")
                .pickupCity("Colombo")
                .pickupPostalCode("00100")
                .recipientName("Bob Smith")
                .recipientPhone("+94773334455")
                .deliveryAddress("20 Beach Road")
                .deliveryCity("Negombo")
                .deliveryPostalCode("11500")
                .weightKg(new BigDecimal("1.25"))
                .lengthCm(new BigDecimal("15.00"))
                .widthCm(new BigDecimal("10.00"))
                .heightCm(new BigDecimal("5.00"))
                .deliveryType(DeliveryType.STANDARD)
                .status(ParcelStatus.CREATED)
                .deliveryFee(new BigDecimal("8.50"))
                .currency("USD")
                .build();

        ParcelStatusHistory history = ParcelStatusHistory.builder()
                .parcel(parcel)
                .newStatus(ParcelStatus.CREATED)
                .changedByUserId(senderId)
                .actorRole("ROLE_CUSTOMER")
                .location("Colombo")
                .notes("Initial booking")
                .build();

        parcel.addStatusHistory(history);
        Parcel saved = parcelRepository.save(parcel);

        assertNotNull(saved.getId());
        Optional<Parcel> found = parcelRepository.findByTrackingNumber("SD-7777-8888-LK");
        assertTrue(found.isPresent());
        assertEquals("Jane Doe", found.get().getSenderName());
        assertEquals(1, found.get().getStatusHistory().size());
        assertTrue(parcelRepository.existsByTrackingNumber("SD-7777-8888-LK"));
    }

    @Test
    @DisplayName("Should find parcels by sender ID with pagination")
    void shouldFindBySenderIdWithPagination() {
        UUID senderId = UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            Parcel parcel = Parcel.builder()
                    .trackingNumber("SD-0000-000" + i + "-LK")
                    .senderId(senderId)
                    .senderType(SenderType.CUSTOMER)
                    .senderName("Multi Sender")
                    .senderPhone("+94770000000")
                    .pickupAddress("A")
                    .pickupCity("Colombo")
                    .pickupPostalCode("00100")
                    .recipientName("Recipient")
                    .recipientPhone("+94771111111")
                    .deliveryAddress("B")
                    .deliveryCity("Galle")
                    .deliveryPostalCode("80000")
                    .weightKg(new BigDecimal("2.00"))
                    .lengthCm(new BigDecimal("10.00"))
                    .widthCm(new BigDecimal("10.00"))
                    .heightCm(new BigDecimal("10.00"))
                    .status(ParcelStatus.CREATED)
                    .deliveryFee(new BigDecimal("10.00"))
                    .currency("USD")
                    .build();
            parcelRepository.save(parcel);
        }

        Page<Parcel> page = parcelRepository.findBySenderId(senderId, PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }
}

