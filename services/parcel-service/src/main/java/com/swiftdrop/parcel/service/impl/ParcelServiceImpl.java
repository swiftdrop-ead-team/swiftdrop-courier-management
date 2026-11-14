package com.swiftdrop.parcel.service.impl;

import com.swiftdrop.parcel.dto.request.CancelParcelRequest;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelStatusRequest;
import com.swiftdrop.parcel.dto.response.PageResponse;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.entity.ParcelStatusHistory;
import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import com.swiftdrop.parcel.event.payload.ParcelCancelledEvent;
import com.swiftdrop.parcel.event.payload.ParcelCreatedEvent;
import com.swiftdrop.parcel.event.payload.ParcelStatusUpdatedEvent;
import com.swiftdrop.parcel.event.publisher.ParcelEventPublisher;
import com.swiftdrop.parcel.exception.InvalidStatusTransitionException;
import com.swiftdrop.parcel.exception.ParcelCancellationException;
import com.swiftdrop.parcel.exception.ResourceNotFoundException;
import com.swiftdrop.parcel.exception.UnauthorizedParcelAccessException;
import com.swiftdrop.parcel.mapper.ParcelMapper;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.security.UserPrincipal;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.parcel.service.PricingClientService;
import com.swiftdrop.parcel.util.TrackingNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;
    private final ParcelMapper parcelMapper;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final PricingClientService pricingClientService;
    private final com.swiftdrop.parcel.service.MerchantEligibility eligibility;
    private final com.swiftdrop.parcel.service.ParcelEstimateService estimates;
    private final ParcelEventPublisher eventPublisher;

    @Override
    @Transactional
    public ParcelResponse createParcel(CreateParcelRequest request, UserPrincipal currentUser) {
        eligibility.ensureApproved(currentUser);
        String merchantFingerprint = null;
        if(request.getMerchantOrderReference()!=null) {
            if(!hasRole(currentUser,"ROLE_MERCHANT")) throw new UnauthorizedParcelAccessException("Merchant reference requires a merchant account");
            var json=new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request);
            ((com.fasterxml.jackson.databind.node.ObjectNode)json).remove("estimateId");
            merchantFingerprint=json.toString();
            var previous=parcelRepository.findBySenderIdAndMerchantOrderReference(currentUser.getId(),request.getMerchantOrderReference());
            if(previous.isPresent()) {
                if(!merchantFingerprint.equals(previous.get().getMerchantRequestFingerprint())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Order reference was already used with a different payload");
                return parcelMapper.toResponse(previous.get());
            }
        }
        log.info("Creating parcel request for user [{}]", currentUser.getId());

        SenderType senderType = hasRole(currentUser, "ROLE_MERCHANT")
                ? SenderType.MERCHANT
                : SenderType.CUSTOMER;

        String trackingNumber = generateUniqueTrackingNumber();

        // 1. Sensible defaults for sender details
        String senderName = StringUtils.hasText(request.getSenderName())
                ? request.getSenderName().trim()
                : (StringUtils.hasText(currentUser.getFullName()) ? currentUser.getFullName() : "SwiftDrop Shipper");
        String senderEmail = StringUtils.hasText(request.getSenderEmail())
                ? request.getSenderEmail().trim()
                : currentUser.getEmail();
        String senderPhone = StringUtils.hasText(request.getSenderPhone())
                ? request.getSenderPhone().trim()
                : "+94112345678";

        // 2. Sensible defaults for pickup location
        String pickupAddress = StringUtils.hasText(request.getPickupAddress())
                ? request.getPickupAddress().trim()
                : "SwiftDrop Regional Intake Hub";
        String pickupCity = StringUtils.hasText(request.getPickupCity())
                ? request.getPickupCity().trim()
                : "Colombo";
        String pickupPostalCode = StringUtils.hasText(request.getPickupPostalCode())
                ? request.getPickupPostalCode().trim()
                : "00100";

        // 3. Sensible defaults for recipient contact
        String recipientName = StringUtils.hasText(request.getRecipientName())
                ? request.getRecipientName().trim()
                : "Valued Recipient";
        String recipientPhone = StringUtils.hasText(request.getRecipientPhone())
                ? request.getRecipientPhone().trim()
                : "+94770000000";
        String recipientEmail = StringUtils.hasText(request.getRecipientEmail())
                ? request.getRecipientEmail().trim()
                : null;

        // 4. Sensible defaults for delivery location
        String deliveryAddress = StringUtils.hasText(request.getDeliveryAddress())
                ? request.getDeliveryAddress().trim()
                : "Metropolitan Delivery Zone";
        String deliveryCity = StringUtils.hasText(request.getDeliveryCity())
                ? request.getDeliveryCity().trim()
                : extractCity(deliveryAddress);
        String deliveryPostalCode = StringUtils.hasText(request.getDeliveryPostalCode())
                ? request.getDeliveryPostalCode().trim()
                : "00000";

        // 5. Sensible defaults for package dimensions
        BigDecimal weightKg = (request.getWeightKg() != null && request.getWeightKg().compareTo(BigDecimal.ZERO) > 0)
                ? request.getWeightKg()
                : new BigDecimal("1.00");
        BigDecimal lengthCm = (request.getLengthCm() != null && request.getLengthCm().compareTo(BigDecimal.ZERO) > 0)
                ? request.getLengthCm()
                : new BigDecimal("10.00");
        BigDecimal widthCm = (request.getWidthCm() != null && request.getWidthCm().compareTo(BigDecimal.ZERO) > 0)
                ? request.getWidthCm()
                : new BigDecimal("10.00");
        BigDecimal heightCm = (request.getHeightCm() != null && request.getHeightCm().compareTo(BigDecimal.ZERO) > 0)
                ? request.getHeightCm()
                : new BigDecimal("10.00");
        DeliveryType deliveryType = request.getDeliveryType() != null
                ? request.getDeliveryType()
                : DeliveryType.STANDARD;

        BigDecimal deliveryFee = request.getEstimateId() != null ? estimates.consume(request, currentUser.getId()) : pricingClientService.calculateDeliveryFee(
                weightKg,
                lengthCm,
                widthCm,
                heightCm,
                deliveryType,
                request.isFragile()
        );

        Parcel parcel = Parcel.builder()
                .merchantOrderReference(request.getMerchantOrderReference()).merchantRequestFingerprint(merchantFingerprint)
                .trackingNumber(trackingNumber)
                .senderId(currentUser.getId())
                .senderType(senderType)
                .senderName(senderName)
                .senderPhone(senderPhone)
                .senderEmail(senderEmail)
                .pickupAddress(pickupAddress)
                .pickupCity(pickupCity)
                .pickupPostalCode(pickupPostalCode)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .recipientEmail(recipientEmail)
                .deliveryAddress(deliveryAddress)
                .deliveryCity(deliveryCity)
                .deliveryPostalCode(deliveryPostalCode)
                .weightKg(weightKg)
                .lengthCm(lengthCm)
                .widthCm(widthCm)
                .heightCm(heightCm)
                .description(request.getDescription())
                .isFragile(request.isFragile())
                .deliveryType(deliveryType)
                .status(ParcelStatus.CREATED)
                .deliveryFee(deliveryFee)
                .currency("LKR")
                .specialInstructions(request.getSpecialInstructions())
                .pickupDate(request.getPickupDate() == null ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Colombo")).toString() : request.getPickupDate())
                .codAmount(request.getCodAmount() == null ? BigDecimal.ZERO : request.getCodAmount())
                .build();

        ParcelStatusHistory initialHistory = ParcelStatusHistory.builder()
                .parcel(parcel)
                .previousStatus(null)
                .newStatus(ParcelStatus.CREATED)
                .changedByUserId(currentUser.getId())
                .actorRole(getPrimaryRole(currentUser))
                .location(pickupCity)
                .notes("Parcel booking created successfully.")
                .build();

        parcel.addStatusHistory(initialHistory);
        Parcel savedParcel = parcelRepository.save(parcel);

        eventPublisher.publishParcelCreated(ParcelCreatedEvent.builder()
                .parcelId(savedParcel.getId())
                .trackingNumber(savedParcel.getTrackingNumber())
                .senderId(savedParcel.getSenderId())
                .senderType(savedParcel.getSenderType())
                .pickupCity(savedParcel.getPickupCity())
                .deliveryCity(savedParcel.getDeliveryCity())
                .weightKg(savedParcel.getWeightKg())
                .deliveryType(savedParcel.getDeliveryType())
                .status(savedParcel.getStatus())
                .deliveryFee(savedParcel.getDeliveryFee())
                .currency(savedParcel.getCurrency())
                .build());

        log.info("Parcel [{}] created successfully with tracking number [{}]", savedParcel.getId(), trackingNumber);
        return parcelMapper.toResponse(savedParcel);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse getParcelById(UUID id, UserPrincipal currentUser) {
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with ID: " + id));

        validateParcelAccess(parcel, currentUser);
        return parcelMapper.toResponse(parcel);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse getParcelByTrackingNumber(String trackingNumber, UserPrincipal currentUser) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with tracking number: " + trackingNumber));

        validateParcelAccess(parcel, currentUser);
        return parcelMapper.toResponse(parcel);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTrackingResponse getPublicTracking(String trackingNumber) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No shipment found matching tracking number: " + trackingNumber));

        return parcelMapper.toPublicTrackingResponse(parcel);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ParcelResponse> getParcelsForCurrentUser(UserPrincipal currentUser, Pageable pageable) {
        Page<Parcel> page = parcelRepository.findBySenderId(currentUser.getId(), pageable);
        return PageResponse.of(page.map(parcelMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ParcelResponse> getAllParcels(ParcelStatus status, Pageable pageable, UserPrincipal currentUser) {
        if (!hasRole(currentUser, "ROLE_ADMIN")) {
            throw new UnauthorizedParcelAccessException("Only administrators may query all system parcels.");
        }

        Page<Parcel> page = (status != null)
                ? parcelRepository.findByStatus(status, pageable)
                : parcelRepository.findAll(pageable);

        return PageResponse.of(page.map(parcelMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ParcelResponse> getParcelsBySender(UUID senderId, ParcelStatus status, Pageable pageable, UserPrincipal currentUser) {
        if (!hasRole(currentUser, "ROLE_ADMIN") && !currentUser.getId().equals(senderId)) {
            throw new UnauthorizedParcelAccessException("Access denied. You cannot view parcels for another sender.");
        }

        Page<Parcel> page = (status != null)
                ? parcelRepository.findBySenderIdAndStatus(senderId, status, pageable)
                : parcelRepository.findBySenderId(senderId, pageable);

        return PageResponse.of(page.map(parcelMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ParcelResponse> getParcelsByCourier(UUID courierId, Pageable pageable, UserPrincipal currentUser) {
        if (!hasRole(currentUser, "ROLE_ADMIN") && !currentUser.getId().equals(courierId)) {
            throw new UnauthorizedParcelAccessException("Access denied. You cannot view parcels assigned to another courier.");
        }

        Page<Parcel> page = parcelRepository.findByAssignedCourierId(courierId, pageable);
        return PageResponse.of(page.map(parcelMapper::toResponse));
    }

    @Override
    @Transactional
    public ParcelResponse updateParcelDetails(UUID id, UpdateParcelRequest request, UserPrincipal currentUser) {
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with ID: " + id));

        if (!hasRole(currentUser, "ROLE_ADMIN") && !parcel.getSenderId().equals(currentUser.getId())) {
            throw new UnauthorizedParcelAccessException("Only the creator or an administrator can update parcel details.");
        }

        if (parcel.getStatus() != ParcelStatus.CREATED) {
            throw new InvalidStatusTransitionException("Cannot update details of a parcel that has already transitioned to " + parcel.getStatus());
        }

        if (StringUtils.hasText(request.getRecipientName())) {
            parcel.setRecipientName(request.getRecipientName().trim());
        }
        if (StringUtils.hasText(request.getRecipientPhone())) {
            parcel.setRecipientPhone(request.getRecipientPhone().trim());
        }
        if (StringUtils.hasText(request.getRecipientEmail())) {
            parcel.setRecipientEmail(request.getRecipientEmail().trim());
        }
        if (StringUtils.hasText(request.getDeliveryAddress())) {
            parcel.setDeliveryAddress(request.getDeliveryAddress().trim());
        }
        if (StringUtils.hasText(request.getDeliveryCity())) {
            parcel.setDeliveryCity(request.getDeliveryCity().trim());
        }
        if (StringUtils.hasText(request.getDeliveryPostalCode())) {
            parcel.setDeliveryPostalCode(request.getDeliveryPostalCode().trim());
        }
        if (request.getDescription() != null) {
            parcel.setDescription(request.getDescription());
        }
        if (request.getSpecialInstructions() != null) {
            parcel.setSpecialInstructions(request.getSpecialInstructions());
        }

        Parcel updated = parcelRepository.save(parcel);
        return parcelMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ParcelResponse updateParcelStatus(UUID id, UpdateParcelStatusRequest request, UserPrincipal currentUser) {
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with ID: " + id));

        ParcelStatus currentStatus = parcel.getStatus();
        ParcelStatus newStatus = request.getNewStatus();

        // 1. Role access verification
        if (hasRole(currentUser, "ROLE_CUSTOMER")) {
            throw new UnauthorizedParcelAccessException("Customers are not authorized to update parcel delivery statuses.");
        }

        validateParcelAccess(parcel, currentUser);
        if (newStatus == ParcelStatus.DELIVERED) {
            throw new InvalidStatusTransitionException("Submit recipient proof through dispatch to complete delivery.");
        }
        if (!hasRole(currentUser, "ROLE_ADMIN") && request.getAssignedCourierId() != null
                && !request.getAssignedCourierId().equals(currentUser.getId())) {
            throw new UnauthorizedParcelAccessException("Only an administrator can reassign a parcel.");
        }
        // 2. Lifecycle transition check
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        // 3. Status-specific validation
        if (newStatus == ParcelStatus.FAILED && !StringUtils.hasText(request.getFailureReason())) {
            throw new InvalidStatusTransitionException("Failure reason is mandatory when marking a parcel as FAILED.");
        }

        parcel.setStatus(newStatus);
        if (StringUtils.hasText(request.getFailureReason())) {
            parcel.setFailureReason(request.getFailureReason());
        }
        if (request.getAssignedCourierId() != null) {
            parcel.setAssignedCourierId(request.getAssignedCourierId());
        }

        ParcelStatusHistory history = ParcelStatusHistory.builder()
                .parcel(parcel)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .changedByUserId(currentUser.getId())
                .actorRole(getPrimaryRole(currentUser))
                .location(request.getLocation() != null ? request.getLocation() : parcel.getDeliveryCity())
                .notes(request.getNotes())
                .build();

        parcel.addStatusHistory(history);
        Parcel saved = parcelRepository.save(parcel);

        eventPublisher.publishParcelStatusUpdated(ParcelStatusUpdatedEvent.builder()
                .parcelId(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .changedByUserId(currentUser.getId())
                .actorRole(getPrimaryRole(currentUser))
                .location(history.getLocation())
                .notes(history.getNotes())
                .assignedCourierId(saved.getAssignedCourierId())
                .build());

        log.info("Parcel [{}] status transitioned from [{}] to [{}] by user [{}]", id, currentStatus, newStatus, currentUser.getId());
        return parcelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ParcelResponse cancelParcel(UUID id, CancelParcelRequest request, UserPrincipal currentUser) {
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with ID: " + id));

        if (!hasRole(currentUser, "ROLE_ADMIN") && !parcel.getSenderId().equals(currentUser.getId())) {
            throw new UnauthorizedParcelAccessException("You are not authorized to cancel this parcel.");
        }

        if (!parcel.getStatus().isCancellable()) {
            throw new ParcelCancellationException(
                    String.format("Parcel cannot be cancelled in its current state [%s]. Cancellations are only permitted prior to dispatch/in-transit.", parcel.getStatus()));
        }

        ParcelStatus previousStatus = parcel.getStatus();
        parcel.setStatus(ParcelStatus.CANCELLED);
        parcel.setFailureReason(request.getReason());

        ParcelStatusHistory history = ParcelStatusHistory.builder()
                .parcel(parcel)
                .previousStatus(previousStatus)
                .newStatus(ParcelStatus.CANCELLED)
                .changedByUserId(currentUser.getId())
                .actorRole(getPrimaryRole(currentUser))
                .location("Sender Cancellation")
                .notes("Cancelled with reason: " + request.getReason())
                .build();

        parcel.addStatusHistory(history);
        Parcel saved = parcelRepository.save(parcel);

        eventPublisher.publishParcelCancelled(ParcelCancelledEvent.builder()
                .parcelId(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .cancelledByUserId(currentUser.getId())
                .actorRole(getPrimaryRole(currentUser))
                .reason(request.getReason())
                .build());

        log.info("Parcel [{}] cancelled by user [{}]", id, currentUser.getId());
        return parcelMapper.toResponse(saved);
    }

    private void validateParcelAccess(Parcel parcel, UserPrincipal currentUser) {
        if (hasRole(currentUser, "ROLE_ADMIN")) {
            return;
        }

        if (hasRole(currentUser, "ROLE_COURIER")) {
            // Courier can view if assigned to it or picking up
            if (parcel.getAssignedCourierId() != null && parcel.getAssignedCourierId().equals(currentUser.getId())) {
                return;
            }
            throw new UnauthorizedParcelAccessException("This parcel is not assigned to you.");
        }

        // Customers and Merchants can only access their own shipments
        if (!parcel.getSenderId().equals(currentUser.getId())) {
            throw new UnauthorizedParcelAccessException("Access denied. You do not own this parcel.");
        }
    }

    private String generateUniqueTrackingNumber() {
        for (int i = 0; i < 5; i++) {
            String trackingNumber = trackingNumberGenerator.generate("LK");
            if (!parcelRepository.existsByTrackingNumber(trackingNumber)) {
                return trackingNumber;
            }
        }
        throw new RuntimeException("Failed to generate a unique tracking number after multiple attempts.");
    }

    private boolean hasRole(UserPrincipal principal, String roleName) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(roleName));
    }

    private String getPrimaryRole(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_CUSTOMER");
    }

    private String extractCity(String address) {
        if (!StringUtils.hasText(address)) {
            return "Colombo";
        }
        String[] parts = address.split("[,\\-]");
        if (parts.length > 1) {
            String candidate = parts[parts.length - 1].trim();
            if (candidate.length() >= 2) {
                return candidate.length() > 100 ? candidate.substring(0, 100) : candidate;
            }
        }
        return "Colombo";
    }
}

