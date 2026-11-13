package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.MerchantOrder;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.exception.ParcelServiceException;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceTest {

    @Mock private MerchantOrderRepository orderRepository;
    @Mock private WebhookDeliveryRepository deliveryRepository;
    @Mock private MerchantProfileService profileService;
    @Mock private ParcelClient parcelClient;
    @InjectMocks private MerchantOrderService service;

    private UUID merchantId;
    private MerchantPrincipal principal;
    private MerchantOrderRequest request;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        principal = new MerchantPrincipal(UUID.randomUUID(), "m@example.test", false);
        request = MerchantOrderRequest.builder()
                .recipientName("Customer")
                .recipientPhone("+94770000000")
                .deliveryAddress("Delivery")
                .pickupAddress("Pickup")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
        lenient().when(profileService.requireApproved(principal))
                .thenReturn(MerchantProfile.builder().id(merchantId).build());
        lenient().when(profileService.requireApprovedForOrder(principal))
                .thenReturn(MerchantProfile.builder().id(merchantId).build());
    }

    @Test
    void repeatedRequestWithSameKeyReturnsLocalOrderWithoutCallingParcelAgain() {
        MerchantOrder order = MerchantOrder.builder().id(12L).merchantId(merchantId).idempotencyKey("key-1")
                .recipientName("Customer").recipientPhone("+94770000000")
                .deliveryAddress("Delivery").pickupAddress("Pickup").deliveryStatus("CREATED").build();
        when(orderRepository.findByMerchantIdAndIdempotencyKey(merchantId, "key-1")).thenReturn(Optional.of(order));

        var response = service.create(principal, "key-1", request);

        assertThat(response.id()).isEqualTo(12L);
        verifyNoInteractions(parcelClient);
    }

    @Test
    void idempotencyKeyCannotBeReusedForDifferentPayload() {
        MerchantOrder order = MerchantOrder.builder().merchantId(merchantId).idempotencyKey("key-1")
                .recipientName("Other").recipientPhone("+94770000000")
                .deliveryAddress("Delivery").pickupAddress("Pickup").build();
        when(orderRepository.findByMerchantIdAndIdempotencyKey(merchantId, "key-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.create(principal, "key-1", request))
                .isInstanceOf(MerchantConflictException.class);
        verifyNoInteractions(parcelClient);
    }

    @Test
    void parcelFailureDoesNotCreateLocalOrder() {
        when(orderRepository.findByMerchantIdAndIdempotencyKey(merchantId, "key-2")).thenReturn(Optional.empty());
        when(parcelClient.createParcel(eq("key-2"), any())).thenThrow(new ParcelServiceException("unavailable"));

        assertThatThrownBy(() -> service.create(principal, "key-2", request))
                .isInstanceOf(ParcelServiceException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void ordersAreQueriedOnlyForAuthenticatedMerchant() {
        when(orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)).thenReturn(List.of());
        service.getOrders(principal);
        verify(orderRepository).findByMerchantIdOrderByCreatedAtDesc(merchantId);
        verify(orderRepository, never()).findAll();
    }
}
