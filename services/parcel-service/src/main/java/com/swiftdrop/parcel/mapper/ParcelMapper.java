package com.swiftdrop.parcel.mapper;

import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.ParcelStatusHistoryResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.entity.ParcelStatusHistory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParcelMapper {

    public Parcel toEntity(CreateParcelRequest request) {
        if (request == null) {
            return null;
        }

        return Parcel.builder()
                .senderName(safeTrim(request.getSenderName()))
                .senderPhone(safeTrim(request.getSenderPhone()))
                .senderEmail(safeTrim(request.getSenderEmail()))
                .pickupAddress(safeTrim(request.getPickupAddress()))
                .pickupCity(safeTrim(request.getPickupCity()))
                .pickupPostalCode(safeTrim(request.getPickupPostalCode()))
                .recipientName(safeTrim(request.getRecipientName()))
                .recipientPhone(safeTrim(request.getRecipientPhone()))
                .recipientEmail(safeTrim(request.getRecipientEmail()))
                .deliveryAddress(safeTrim(request.getDeliveryAddress()))
                .deliveryCity(safeTrim(request.getDeliveryCity()))
                .deliveryPostalCode(safeTrim(request.getDeliveryPostalCode()))
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .description(request.getDescription())
                .isFragile(request.isFragile())
                .deliveryType(request.getDeliveryType())
                .specialInstructions(request.getSpecialInstructions())
                .pickupDate(request.getPickupDate())
                .codAmount(request.getCodAmount() == null ? java.math.BigDecimal.ZERO : request.getCodAmount())
                .build();
    }

    public ParcelResponse toResponse(Parcel parcel) {
        if (parcel == null) {
            return null;
        }

        List<ParcelStatusHistoryResponse> historyResponses = parcel.getStatusHistory() != null
                ? parcel.getStatusHistory().stream()
                        .map(this::toStatusHistoryResponse)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return ParcelResponse.builder()
                .id(parcel.getId())
                .trackingNumber(parcel.getTrackingNumber())
                .senderId(parcel.getSenderId())
                .senderType(parcel.getSenderType())
                .senderName(parcel.getSenderName())
                .senderPhone(parcel.getSenderPhone())
                .senderEmail(parcel.getSenderEmail())
                .pickupAddress(parcel.getPickupAddress())
                .pickupCity(parcel.getPickupCity())
                .pickupPostalCode(parcel.getPickupPostalCode())
                .recipientName(parcel.getRecipientName())
                .recipientPhone(parcel.getRecipientPhone())
                .recipientEmail(parcel.getRecipientEmail())
                .deliveryAddress(parcel.getDeliveryAddress())
                .deliveryCity(parcel.getDeliveryCity())
                .deliveryPostalCode(parcel.getDeliveryPostalCode())
                .weightKg(parcel.getWeightKg())
                .lengthCm(parcel.getLengthCm())
                .widthCm(parcel.getWidthCm())
                .heightCm(parcel.getHeightCm())
                .description(parcel.getDescription())
                .isFragile(parcel.isFragile())
                .deliveryType(parcel.getDeliveryType())
                .status(parcel.getStatus())
                .deliveryFee(parcel.getDeliveryFee())
                .currency(parcel.getCurrency())
                .assignedCourierId(parcel.getAssignedCourierId())
                .failureReason(parcel.getFailureReason())
                .specialInstructions(parcel.getSpecialInstructions())
                .pickupDate(parcel.getPickupDate())
                .codAmount(parcel.getCodAmount())
                .createdAt(parcel.getCreatedAt())
                .updatedAt(parcel.getUpdatedAt())
                .statusHistory(historyResponses)
                .build();
    }

    public PublicTrackingResponse toPublicTrackingResponse(Parcel parcel) {
        if (parcel == null) {
            return null;
        }

        List<PublicTrackingResponse.PublicStatusCheckpoint> checkpoints = parcel.getStatusHistory() != null
                ? parcel.getStatusHistory().stream()
                        .map(history -> PublicTrackingResponse.PublicStatusCheckpoint.builder()
                                .status(history.getNewStatus())
                                .location(history.getLocation())
                                .notes(history.getNotes())
                                .timestamp(history.getTimestamp())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return PublicTrackingResponse.builder()
                .trackingNumber(parcel.getTrackingNumber())
                .status(parcel.getStatus())
                .deliveryType(parcel.getDeliveryType())
                .originCity(parcel.getPickupCity())
                .destinationCity(parcel.getDeliveryCity())
                .isFragile(parcel.isFragile())
                .createdAt(parcel.getCreatedAt())
                .updatedAt(parcel.getUpdatedAt())
                .checkpoints(checkpoints)
                .build();
    }

    public ParcelStatusHistoryResponse toStatusHistoryResponse(ParcelStatusHistory history) {
        if (history == null) {
            return null;
        }

        return ParcelStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .changedByUserId(history.getChangedByUserId())
                .actorRole(history.getActorRole())
                .location(history.getLocation())
                .notes(history.getNotes())
                .timestamp(history.getTimestamp())
                .build();
    }

    private String safeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
