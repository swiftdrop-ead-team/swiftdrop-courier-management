package com.swiftdrop.merchant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.*;
import com.swiftdrop.merchant.messaging.MerchantParcelEventListener;
import com.swiftdrop.merchant.repository.*;
import com.swiftdrop.merchant.service.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantWorkflowIntegrationTest {

    private static final String TEST_JWT_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MerchantProfileRepository profileRepository;
    @Autowired private MerchantOrderRepository orderRepository;
    @Autowired private MerchantApiKeyRepository apiKeyRepository;
    @Autowired private WebhookSubscriptionRepository subscriptionRepository;
    @Autowired private WebhookDeliveryRepository deliveryRepository;
    @Autowired private ProcessedParcelEventRepository processedEventRepository;
    @Autowired private MerchantProfileService profileService;
    @Autowired private MerchantApiKeyService apiKeyService;
    @Autowired private WebhookSubscriptionService subscriptionService;
    @Autowired private MerchantParcelEventService eventService;
    @Autowired private MerchantParcelEventListener eventListener;
    @Autowired private WebhookDeliveryService deliveryService;
    @Autowired private WebhookCryptoService cryptoService;

    @MockitoBean
    private ParcelClient parcelClient;

    private static HttpServer receiver;
    private static final ConcurrentLinkedQueue<Integer> receiverResponses = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger receivedCount = new AtomicInteger();
    private static final AtomicReference<String> receivedPayload = new AtomicReference<>();
    private static final AtomicReference<String> receivedSignature = new AtomicReference<>();
    private static String receiverUrl;

    @BeforeAll
    static void startDemoReceiver() throws IOException {
        receiver = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        receiver.createContext("/hook", exchange -> {
            receivedPayload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedSignature.set(exchange.getRequestHeaders().getFirst("X-SwiftDrop-Signature"));
            receivedCount.incrementAndGet();
            Integer nextStatus = receiverResponses.poll();
            int status = nextStatus == null ? 204 : nextStatus;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        receiver.start();
        receiverUrl = "http://127.0.0.1:" + receiver.getAddress().getPort() + "/hook";
    }

    @AfterAll
    static void stopDemoReceiver() {
        if (receiver != null) {
            receiver.stop(0);
        }
    }

    @BeforeEach
    void cleanDatabase() {
        reset(parcelClient);
        deliveryRepository.deleteAll();
        subscriptionRepository.deleteAll();
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();
        apiKeyRepository.deleteAll();
        profileRepository.deleteAll();
        receiverResponses.clear();
        receivedCount.set(0);
        receivedPayload.set(null);
        receivedSignature.set(null);
    }

    @Test
    void merchantRegistrationApprovalAndAuthorizationFlow() throws Exception {
        UUID merchantUser = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/merchant/profile")
                        .header("Authorization", bearer(merchantUser, "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessName":"Northwind","contactPerson":"Ada","phoneNumber":"+94770000000",
                                 "businessAddress":"1 Market Road"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"));

        MerchantProfile pending = profileRepository.findByUserId(merchantUser).orElseThrow();
        mockMvc.perform(patch("/api/v1/merchant/admin/registrations/" + pending.getId() + "/approve")
                        .header("Authorization", bearer(UUID.randomUUID(), "ROLE_MERCHANT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/merchant/admin/registrations/" + pending.getId() + "/approve")
                        .header("Authorization", bearer(UUID.randomUUID(), "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));

        assertThat(profileService.getByUserId(merchantUser).getApprovalStatus())
                .isEqualTo(MerchantApprovalStatus.APPROVED);
    }

    @Test
    void unapprovedMerchantCannotReadOrders() throws Exception {
        UUID owner = UUID.randomUUID();
        saveProfile(owner, MerchantApprovalStatus.PENDING);
        mockMvc.perform(get("/api/v1/merchant/orders")
                        .header("Authorization", bearer(owner, "ROLE_MERCHANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void merchantCannotSeeAnotherMerchantsOrders() throws Exception {
        MerchantProfile merchantA = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        MerchantProfile merchantB = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        saveOrder(merchantA.getId(), "A-1", "parcel-a");
        saveOrder(merchantB.getId(), "B-1", "parcel-b");

        mockMvc.perform(get("/api/v1/merchant/orders")
                        .header("Authorization", bearer(merchantA.getUserId(), "ROLE_MERCHANT")
                                .replace("Bearer ", "bEaReR ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].parcelId").value("parcel-a"));
    }

    @Test
    void jwtAuthenticationRequiresIdentityIssuerAndKnownRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/merchant/profile")
                        .header("Authorization", bearerWithIssuer(userId, "ROLE_MERCHANT", "untrusted-issuer")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/merchant/profile")
                        .header("Authorization", bearer(userId, "ROLE_SUPERUSER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardReportsOwnOrderAndWebhookTotals() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        saveOrder(merchant.getId(), "dashboard-1", "parcel-dashboard-1");
        MerchantProfile otherMerchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        MerchantOrder otherOrder = saveOrder(otherMerchant.getId(), "dashboard-other", "parcel-dashboard-other");
        var subscription = createSubscription(merchant);
        processedEventRepository.save(ProcessedParcelEvent.builder()
                .eventKey("dashboard-event")
                .parcelId(otherOrder.getParcelId())
                .eventType("DELIVERY_STATUS_UPDATED")
                .occurredAt(LocalDateTime.now())
                .build());
        deliveryRepository.save(WebhookDelivery.builder()
                .webhookId(subscription.id())
                .eventKey("dashboard-event")
                .orderId(orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), "dashboard-1")
                        .orElseThrow().getId())
                .eventPayload("{}")
                .status(WebhookDeliveryStatus.DELIVERED)
                .attemptCount(1)
                .nextAttemptAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/v1/merchant/orders/dashboard/summary")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.successfulWebhookDeliveries").value(1))
                .andExpect(jsonPath("$.data.failedWebhookDeliveries").value(0));
    }

    @Test
    void invalidOrderRequestIsRejectedByValidation() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        mockMvc.perform(post("/api/v1/merchant/orders")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT"))
                        .header("Idempotency-Key", "order-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientName\":\"\",\"recipientPhone\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verifyNoInteractions(parcelClient);
    }

    @Test
    void orderCreationIsIdempotentAndParcelFailureLeavesNoLocalOrder() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        when(parcelClient.createParcel(eq("order-123"), any(ParcelOrderRequest.class)))
                .thenReturn(new ParcelOrderResponse("parcel-123", "TRACK-123"));
        String body = """
                {"recipientName":"Sam","recipientPhone":"+94770000000","deliveryAddress":"4 Main Street",
                 "pickupAddress":"Merchant Hub","description":"Books","weightKg":1.5}
                """;

        mockMvc.perform(post("/api/v1/merchant/orders")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT"))
                        .header("Idempotency-Key", "order-123")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parcelId").value("parcel-123"));
        mockMvc.perform(post("/api/v1/merchant/orders")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT"))
                        .header("Idempotency-Key", "order-123")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(parcelClient, times(1)).createParcel(eq("order-123"), any(ParcelOrderRequest.class));
        assertThat(orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), "order-123")).isPresent();

        when(parcelClient.createParcel(eq("order-fails"), any(ParcelOrderRequest.class)))
                .thenThrow(new com.swiftdrop.merchant.exception.ParcelServiceException("unavailable"));
        mockMvc.perform(post("/api/v1/merchant/orders")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT"))
                        .header("Idempotency-Key", "order-fails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), "order-fails")).isEmpty();
    }

    @Test
    void issuedApiKeyAuthenticatesAndListsOnlyPrefix() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        saveOrder(merchant.getId(), "existing", "parcel-existing");
        var issued = apiKeyService.issue(new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false),
                MerchantApiKeyRequest.builder().scopes(java.util.Set.of("orders:read")).build());

        mockMvc.perform(get("/api/v1/merchant/orders").header("X-API-Key", issued.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/v1/merchant/orders")
                        .header("X-API-Key", issued.key())
                        .header("Idempotency-Key", "not-authorized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientName":"Sam","recipientPhone":"+94770000000",
                                 "deliveryAddress":"4 Main Street","pickupAddress":"Merchant Hub"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/merchant/api-keys")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].keyPrefix").value(issued.keyPrefix()))
                .andExpect(jsonPath("$.data[0].keyHash").doesNotExist());

        mockMvc.perform(get("/api/v1/merchant/profile").header("X-API-Key", issued.key()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/merchant/webhooks").header("X-API-Key", issued.key()))
                .andExpect(status().isForbidden());

        apiKeyService.revoke(new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false), issued.id());
        mockMvc.perform(get("/api/v1/merchant/orders").header("X-API-Key", issued.key()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiKeyCannotGainAdminAuthorityFromInvalidStoredScope() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        var issued = apiKeyService.issue(new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false),
                MerchantApiKeyRequest.builder().scopes(java.util.Set.of("orders:read")).build());
        MerchantApiKey storedKey = apiKeyRepository
                .findByKeyPrefixAndRevokedAtIsNull(issued.keyPrefix()).orElseThrow();
        storedKey.setScopes("ROLE_ADMIN");
        apiKeyRepository.save(storedKey);

        mockMvc.perform(get("/api/v1/merchant/admin/registrations/pending")
                        .header("X-API-Key", issued.key()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiKeyIsRejectedWhenMerchantIsNoLongerApproved() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        var issued = apiKeyService.issue(new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false),
                MerchantApiKeyRequest.builder().scopes(java.util.Set.of("orders:read")).build());
        merchant.setApprovalStatus(MerchantApprovalStatus.REJECTED);
        profileRepository.save(merchant);

        mockMvc.perform(get("/api/v1/merchant/orders").header("X-API-Key", issued.key()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoWebhookReceiverAcceptsAndAcknowledgesSignedWebhookHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/demo/webhook")
                        .header("X-SwiftDrop-Event-Id", "event-demo")
                        .header("X-SwiftDrop-Signature", "sha256=deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"parcel.status.changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signaturePresent").value(true))
                .andExpect(jsonPath("$.data.eventId").value("event-demo"));
    }

    @Test
    void webhookCanBeUpdatedListedAndDisabledWithoutExposingPersistedSecret() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        WebhookSubscriptionResponse created = createSubscription(merchant);
        String requestBody = objectMapper.writeValueAsString(WebhookSubscriptionRequest.builder()
                .targetUrl(receiverUrl)
                .eventTypes(java.util.Set.of(MerchantParcelEventService.WEBHOOK_EVENT_TYPE))
                .build());

        mockMvc.perform(put("/api/v1/merchant/webhooks/" + created.id())
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signingSecret").isNotEmpty());

        mockMvc.perform(get("/api/v1/merchant/webhooks")
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].signingSecret").doesNotExist());

        mockMvc.perform(delete("/api/v1/merchant/webhooks/" + created.id())
                        .header("Authorization", bearer(merchant.getUserId(), "ROLE_MERCHANT")))
                .andExpect(status().isOk());
        assertThat(subscriptionRepository.findById(created.id()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void eventDeliveryRetriesWithHmacSignatureThenSucceeds() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        MerchantOrder order = saveOrder(merchant.getId(), "event-order", "parcel-event");
        var subscription = createSubscription(merchant);
        receiverResponses.add(500);
        receiverResponses.add(204);

        DeliveryStatusUpdatedEvent event = statusEvent("evt-retry", "parcel-event", "IN_TRANSIT", 2L);
        eventService.process(event, null);
        assertThat(deliveryRepository.count()).isEqualTo(1);

        deliveryService.processDueDeliveries();
        WebhookDelivery delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);

        delivery.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        deliveryRepository.save(delivery);
        deliveryService.processDueDeliveries();
        delivery = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getDeliveryStatus())
                .isEqualTo("IN_TRANSIT");
        String expected = "sha256=" + cryptoService.sign(subscription.signingSecret(), receivedPayload.get());
        assertThat(receivedSignature.get()).isEqualTo(expected);
        JsonNode payload = objectMapper.readTree(receivedPayload.get());
        assertThat(payload.get("eventType").asText()).isEqualTo("parcel.status.changed");
    }

    @Test
    void webhookDeliveryFailsAfterMaximumAttemptsAndCanBeManuallyRedelivered() {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        saveOrder(merchant.getId(), "max-order", "parcel-max");
        var subscription = createSubscription(merchant);
        receiverResponses.add(500);
        receiverResponses.add(500);
        receiverResponses.add(500);
        eventService.process(statusEvent("evt-max", "parcel-max", "OUT_FOR_DELIVERY", 1L), null);

        for (int attempt = 0; attempt < 3; attempt++) {
            WebhookDelivery delivery = deliveryRepository.findAll().get(0);
            delivery.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
            deliveryRepository.save(delivery);
            deliveryService.processDueDeliveries();
        }
        WebhookDelivery failed = deliveryRepository.findAll().get(0);
        assertThat(failed.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(3);

        receiverResponses.add(204);
        var redelivered = subscriptionService.redeliver(
                new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false),
                subscription.id(), failed.getId());
        assertThat(redelivered.status()).isEqualTo("DELIVERED");
    }

    @Test
    void duplicateEventsCreateOneDeliveryAndStaleVersionsDoNotRegressStatus() throws Exception {
        MerchantProfile merchant = saveProfile(UUID.randomUUID(), MerchantApprovalStatus.APPROVED);
        MerchantOrder order = saveOrder(merchant.getId(), "event-order", "parcel-duplicate");
        createSubscription(merchant);
        DeliveryStatusUpdatedEvent newer = statusEvent("evt-newer", "parcel-duplicate", "DELIVERED", 5L);

        eventListener.onDeliveryStatusUpdated(newer, new org.springframework.amqp.core.Message(new byte[0],
                new org.springframework.amqp.core.MessageProperties()));
        eventService.process(newer, null);
        eventService.process(statusEvent("evt-older", "parcel-duplicate", "IN_TRANSIT", 3L), null);

        assertThat(deliveryRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getDeliveryStatus()).isEqualTo("DELIVERED");
    }

    private MerchantProfile saveProfile(UUID userId, MerchantApprovalStatus status) {
        return profileRepository.save(MerchantProfile.builder()
                .userId(userId)
                .email(userId + "@example.test")
                .businessName("Merchant " + userId.toString().substring(0, 8))
                .contactPerson("Demo User")
                .phoneNumber("+94770000000")
                .businessAddress("1 Market Road")
                .approvalStatus(status)
                .build());
    }

    private MerchantOrder saveOrder(UUID merchantId, String idempotencyKey, String parcelId) {
        return orderRepository.save(MerchantOrder.builder()
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .parcelId(parcelId)
                .trackingNumber("TRACK-" + parcelId)
                .recipientName("Demo recipient")
                .recipientPhone("+94771111111")
                .deliveryAddress("Delivery address")
                .pickupAddress("Pickup address")
                .deliveryStatus("CREATED")
                .build());
    }

    private WebhookSubscriptionResponse createSubscription(MerchantProfile merchant) {
        return subscriptionService.create(
                new MerchantPrincipal(merchant.getUserId(), merchant.getEmail(), false),
                WebhookSubscriptionRequest.builder()
                        .targetUrl(receiverUrl)
                        .eventTypes(java.util.Set.of(MerchantParcelEventService.WEBHOOK_EVENT_TYPE))
                        .build());
    }

    private DeliveryStatusUpdatedEvent statusEvent(String id, String parcelId, String status, long version) {
        return DeliveryStatusUpdatedEvent.builder()
                .eventId(id)
                .eventType("DELIVERY_STATUS_UPDATED")
                .parcelId(parcelId)
                .trackingNumber("TRACK-" + parcelId)
                .previousStatus("CREATED")
                .newStatus(status)
                .timestamp(LocalDateTime.now().plusSeconds(version))
                .version(version)
                .build();
    }

    private String bearer(UUID userId, String role) {
        return bearerWithIssuer(userId, role, "swiftdrop-identity");
    }

    private String bearerWithIssuer(UUID userId, String role, String issuer) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));
        return "Bearer " + Jwts.builder()
                .subject(userId.toString())
                .claim("email", userId + "@example.test")
                .claim("roles", List.of(role))
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }
}
