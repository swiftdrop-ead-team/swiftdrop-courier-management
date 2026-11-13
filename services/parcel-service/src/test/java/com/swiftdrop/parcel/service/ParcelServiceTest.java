package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.dto.request.CancelParcelRequest;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelStatusRequest;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.entity.ParcelStatusHistory;
import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import com.swiftdrop.parcel.event.publisher.ParcelEventPublisher;
import com.swiftdrop.parcel.exception.InvalidStatusTransitionException;
import com.swiftdrop.parcel.exception.ParcelCancellationException;
import com.swiftdrop.parcel.exception.ResourceNotFoundException;
import com.swiftdrop.parcel.exception.UnauthorizedParcelAccessException;
import com.swiftdrop.parcel.mapper.ParcelMapper;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.repository.ParcelStatusHistoryRepository;
import com.swiftdrop.parcel.security.UserPrincipal;
import com.swiftdrop.parcel.service.impl.ParcelServiceImpl;
import com.swiftdrop.parcel.util.TrackingNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelServiceTest {

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private ParcelStatusHistoryRepository historyRepository;

    @Spy
    private ParcelMapper parcelMapper = new ParcelMapper();

    @Mock
    private TrackingNumberGenerator trackingNumberGenerator;

    @Mock
    private PricingClientService pricingClientService;

    @Mock
    private ParcelEventPublisher eventPublisher;

    @InjectMocks
    private ParcelServiceImpl parcelService;

    private UserPrincipal customerUser;
    private UserPrincipal merchantUser;
    private UserPrincipal courierUser;
    private UserPrincipal adminUser;
    private CreateParcelRequest standardCreateRequest;
    private Parcel sampleParcel;

    @BeforeEach
    void setUp() {
        UUID customerId = UUID.randomUUID();
        customerUser = UserPrincipal.builder()
                .id(customerId)
                .email("customer@swiftdrop.io")
                .fullName("John Doe")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        merchantUser = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("merchant@swiftdrop.io")
                .fullName("Apex Store")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")))
                .build();

        courierUser = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("courier@swiftdrop.io")
                .fullName("Swift Courier")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COURIER")))
                .build();

        adminUser = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("admin@swiftdrop.io")
                .fullName("Admin")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        standardCreateRequest = CreateParcelRequest.builder()
                .senderName("John Doe")
                .senderPhone("+94771234567")
                .senderEmail("customer@swiftdrop.io")
                .pickupAddress("123 Galle Road")
                .pickupCity("Colombo")
                .pickupPostalCode("00300")
                .recipientName("Alice Smith")
                .recipientPhone("+94779876543")
                .recipientEmail("alice@example.com")
                .deliveryAddress("456 Kandy Road")
                .deliveryCity("Kandy")
                .deliveryPostalCode("20000")
                .weightKg(new BigDecimal("2.50"))
                .lengthCm(new BigDecimal("20.00"))
                .widthCm(new BigDecimal("15.00"))
                .heightCm(new BigDecimal("10.00"))
                .description("Fragile Electronics")
                .isFragile(true)
                .deliveryType(DeliveryType.STANDARD)
                .build();

        sampleParcel = Parcel.builder()
                .id(UUID.randomUUID())
                .trackingNumber("SD-1234-5678-LK")
                .senderId(customerId)
                .senderType(SenderType.CUSTOMER)
                .senderName("John Doe")
                .senderPhone("+94771234567")
                .pickupAddress("123 Galle Road")
                .pickupCity("Colombo")
                .pickupPostalCode("00300")
                .recipientName("Alice Smith")
                .recipientPhone("+94779876543")
                .deliveryAddress("456 Kandy Road")
                .deliveryCity("Kandy")
                .deliveryPostalCode("20000")
                .weightKg(new BigDecimal("2.50"))
                .lengthCm(new BigDecimal("20.00"))
                .widthCm(new BigDecimal("15.00"))
                .heightCm(new BigDecimal("10.00"))
                .isFragile(true)
                .deliveryType(DeliveryType.STANDARD)
                .status(ParcelStatus.CREATED)
                .deliveryFee(new BigDecimal("15.50"))
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a parcel for a customer with CREATED status and generated tracking number")
    void testCreateParcelSuccessCustomer() {
        when(trackingNumberGenerator.generate("LK")).thenReturn("SD-8888-9999-LK");
        when(parcelRepository.existsByTrackingNumber("SD-8888-9999-LK")).thenReturn(false);
        when(pricingClientService.calculateDeliveryFee(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new BigDecimal("18.50"));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ParcelResponse response = parcelService.createParcel(standardCreateRequest, customerUser);

        assertNotNull(response);
        assertEquals("SD-8888-9999-LK", response.getTrackingNumber());
        assertEquals(ParcelStatus.CREATED, response.getStatus());
        assertEquals(SenderType.CUSTOMER, response.getSenderType());
        assertEquals(new BigDecimal("18.50"), response.getDeliveryFee());

        verify(eventPublisher, times(1)).publishParcelCreated(any());
        verify(parcelRepository, times(1)).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Should create parcel with MERCHANT sender type when invoked by merchant")
    void testCreateParcelSuccessMerchant() {
        when(trackingNumberGenerator.generate("LK")).thenReturn("SD-1111-2222-LK");
        when(parcelRepository.existsByTrackingNumber("SD-1111-2222-LK")).thenReturn(false);
        when(pricingClientService.calculateDeliveryFee(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new BigDecimal("12.00"));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ParcelResponse response = parcelService.createParcel(standardCreateRequest, merchantUser);

        assertEquals(SenderType.MERCHANT, response.getSenderType());
    }

    @Test
    @DisplayName("Should support simplified quick booking from CustomerDashboard modal and apply sensible defaults")
    void testCreateParcelQuickBookingFromFrontend() {
        CreateParcelRequest quickRequest = CreateParcelRequest.builder()
                .recipientName("Michael Scott")
                .deliveryAddress("1725 Slough Avenue, Scranton")
                .weightKg(new BigDecimal("1.50"))
                .deliveryType(DeliveryType.STANDARD)
                .build();

        when(trackingNumberGenerator.generate("LK")).thenReturn("SD-9999-0000-LK");
        when(parcelRepository.existsByTrackingNumber("SD-9999-0000-LK")).thenReturn(false);
        when(pricingClientService.calculateDeliveryFee(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new BigDecimal("10.50"));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ParcelResponse response = parcelService.createParcel(quickRequest, customerUser);

        assertNotNull(response);
        assertEquals("SD-9999-0000-LK", response.getTrackingNumber());
        assertEquals("Michael Scott", response.getRecipientName());
        assertEquals("John Doe", response.getSenderName()); // Defaulted from userPrincipal
        assertEquals("Scranton", response.getDeliveryCity()); // Extracted from address
        assertEquals("Colombo", response.getPickupCity()); // Defaulted
        assertEquals(new BigDecimal("10.00"), response.getLengthCm()); // Defaulted dimension
        assertEquals(ParcelStatus.CREATED, response.getStatus());
    }

    @Test
    @DisplayName("Should retrieve parcel by ID when accessed by owner")
    void testGetParcelByIdOwner() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        ParcelResponse response = parcelService.getParcelById(sampleParcel.getId(), customerUser);
        assertNotNull(response);
        assertEquals(sampleParcel.getId(), response.getId());
    }

    @Test
    @DisplayName("Should throw UnauthorizedParcelAccessException when customer tries to access another customer's parcel")
    void testGetParcelByIdUnauthorizedCustomer() {
        UserPrincipal anotherCustomer = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("other@swiftdrop.io")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        assertThrows(UnauthorizedParcelAccessException.class,
                () -> parcelService.getParcelById(sampleParcel.getId(), anotherCustomer));
    }

    @Test
    @DisplayName("Should allow Admin to access any parcel by ID")
    void testGetParcelByIdAdminAllowed() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        ParcelResponse response = parcelService.getParcelById(sampleParcel.getId(), adminUser);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parcel ID does not exist")
    void testGetParcelByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(parcelRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> parcelService.getParcelById(nonExistentId, customerUser));
    }

    @Test
    @DisplayName("Public tracking lookup should return sanitized data without recipient PII")
    void testGetPublicTrackingSanitized() {
        when(parcelRepository.findByTrackingNumber("SD-1234-5678-LK")).thenReturn(Optional.of(sampleParcel));

        PublicTrackingResponse response = parcelService.getPublicTracking("SD-1234-5678-LK");

        assertNotNull(response);
        assertEquals("SD-1234-5678-LK", response.getTrackingNumber());
        assertEquals("Colombo", response.getOriginCity());
        assertEquals("Kandy", response.getDestinationCity());
        assertEquals(ParcelStatus.CREATED, response.getStatus());
    }

    @Test
    @DisplayName("Should successfully transition CREATED to PICKED_UP by Courier")
    void testUpdateStatusCreatedToPickedUp() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(sampleParcel);

        UpdateParcelStatusRequest request = UpdateParcelStatusRequest.builder()
                .newStatus(ParcelStatus.PICKED_UP)
                .location("Colombo Sorting Hub")
                .notes("Package picked up from sender premises")
                .build();

        ParcelResponse response = parcelService.updateParcelStatus(sampleParcel.getId(), request, courierUser);

        assertNotNull(response);
        assertEquals(ParcelStatus.PICKED_UP, sampleParcel.getStatus());
        verify(eventPublisher, times(1)).publishParcelStatusUpdated(any());
    }

    @Test
    @DisplayName("Should disallow invalid status transition from IN_TRANSIT directly to DELIVERED without OUT_FOR_DELIVERY")
    void testDisallowInvalidTransitionInTransitToDelivered() {
        sampleParcel.setStatus(ParcelStatus.IN_TRANSIT);
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        UpdateParcelStatusRequest request = UpdateParcelStatusRequest.builder()
                .newStatus(ParcelStatus.DELIVERED)
                .build();

        assertThrows(InvalidStatusTransitionException.class,
                () -> parcelService.updateParcelStatus(sampleParcel.getId(), request, courierUser));
    }

    @Test
    @DisplayName("Should disallow any status transition from terminal DELIVERED status")
    void testDisallowTransitionFromDelivered() {
        sampleParcel.setStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        UpdateParcelStatusRequest request = UpdateParcelStatusRequest.builder()
                .newStatus(ParcelStatus.IN_TRANSIT)
                .build();

        assertThrows(InvalidStatusTransitionException.class,
                () -> parcelService.updateParcelStatus(sampleParcel.getId(), request, courierUser));
    }

    @Test
    @DisplayName("Should prevent Customer role from updating courier-controlled delivery statuses")
    void testPreventCustomerFromUpdatingStatus() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        UpdateParcelStatusRequest request = UpdateParcelStatusRequest.builder()
                .newStatus(ParcelStatus.PICKED_UP)
                .build();

        assertThrows(UnauthorizedParcelAccessException.class,
                () -> parcelService.updateParcelStatus(sampleParcel.getId(), request, customerUser));
    }

    @Test
    @DisplayName("Should successfully cancel parcel in CREATED status by owner")
    void testCancelParcelSuccess() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(sampleParcel);

        CancelParcelRequest request = CancelParcelRequest.builder()
                .reason("Customer changed mind")
                .build();

        ParcelResponse response = parcelService.cancelParcel(sampleParcel.getId(), request, customerUser);

        assertEquals(ParcelStatus.CANCELLED, sampleParcel.getStatus());
        assertEquals("Customer changed mind", sampleParcel.getFailureReason());
        verify(eventPublisher, times(1)).publishParcelCancelled(any());
    }

    @Test
    @DisplayName("Should throw ParcelCancellationException when attempting to cancel parcel in IN_TRANSIT status")
    void testCancelParcelFailsWhenInTransit() {
        sampleParcel.setStatus(ParcelStatus.IN_TRANSIT);
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        CancelParcelRequest request = CancelParcelRequest.builder()
                .reason("Too late")
                .build();

        assertThrows(ParcelCancellationException.class,
                () -> parcelService.cancelParcel(sampleParcel.getId(), request, customerUser));
    }

    @Test
    @DisplayName("Should update parcel details when in CREATED status by owner")
    void testUpdateParcelDetailsSuccess() {
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(sampleParcel);

        UpdateParcelRequest request = UpdateParcelRequest.builder()
                .recipientName("Alice Wonderland")
                .specialInstructions("Ring bell twice")
                .build();

        ParcelResponse response = parcelService.updateParcelDetails(sampleParcel.getId(), request, customerUser);

        assertNotNull(response);
        assertEquals("Alice Wonderland", sampleParcel.getRecipientName());
        assertEquals("Ring bell twice", sampleParcel.getSpecialInstructions());
    }

    @Test
    @DisplayName("Should fail to update parcel details when parcel has already been picked up")
    void testUpdateParcelDetailsFailsWhenPickedUp() {
        sampleParcel.setStatus(ParcelStatus.PICKED_UP);
        when(parcelRepository.findById(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));

        UpdateParcelRequest request = UpdateParcelRequest.builder()
                .recipientName("Alice Wonderland")
                .build();

        assertThrows(InvalidStatusTransitionException.class,
                () -> parcelService.updateParcelDetails(sampleParcel.getId(), request, customerUser));
    }
}

