package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.entity.*;
import com.swiftdrop.dispatch.event.CourierAssignedEvent;
import com.swiftdrop.dispatch.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.dispatch.exception.CourierOverloadException;
import com.swiftdrop.dispatch.exception.CourierUnavailableException;
import com.swiftdrop.dispatch.exception.InvalidAssignmentException;
import com.swiftdrop.dispatch.exception.ResourceNotFoundException;
import com.swiftdrop.dispatch.repository.CourierRepository;
import com.swiftdrop.dispatch.repository.DispatchAssignmentRepository;
import com.swiftdrop.dispatch.repository.ProofOfDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchAssignmentServiceTest {

    @Mock
    private DispatchAssignmentRepository assignmentRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private ProofOfDeliveryRepository proofOfDeliveryRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private DispatchAssignmentServiceImpl assignmentService;

    private Courier availableCourier;
    private Courier fullCourier;
    private Courier offlineCourier;
    private DispatchAssignment sampleAssignment;

    @BeforeEach
    void setUp() {
        availableCourier = Courier.builder()
                .id(1L)
                .userId("courier-001")
                .fullName("Kasun Perera")
                .phoneNumber("+94771234567")
                .vehicleType(VehicleType.MOTORBIKE)
                .status(CourierStatus.AVAILABLE)
                .maxActiveDeliveries(5)
                .currentActiveDeliveries(2)
                .build();

        fullCourier = Courier.builder()
                .id(2L)
                .userId("courier-002")
                .fullName("Nimal Silva")
                .phoneNumber("+94719876543")
                .vehicleType(VehicleType.VAN)
                .status(CourierStatus.ASSIGNED)
                .maxActiveDeliveries(3)
                .currentActiveDeliveries(3)
                .build();

        offlineCourier = Courier.builder()
                .id(3L)
                .userId("courier-003")
                .fullName("Amara Offline")
                .status(CourierStatus.OFFLINE)
                .maxActiveDeliveries(5)
                .currentActiveDeliveries(0)
                .build();

        sampleAssignment = DispatchAssignment.builder()
                .id(100L)
                .parcelId("p-123")
                .courier(availableCourier)
                .trackingNumber("SWIFT-001")
                .pickupAddress("Colombo 03")
                .deliveryAddress("Dehiwala")
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should assign courier successfully and publish RabbitMQ event")
    void shouldAssignCourierSuccessfully() {
        AssignCourierRequest request = AssignCourierRequest.builder()
                .parcelId("p-123")
                .courierId(1L)
                .trackingNumber("SWIFT-001")
                .pickupAddress("Colombo 03")
                .deliveryAddress("Dehiwala")
                .recipientName("Ruwan Silva")
                .recipientPhone("+94770001122")
                .build();

        when(courierRepository.findById(1L)).thenReturn(Optional.of(availableCourier));
        when(assignmentRepository.findByParcelId("p-123")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(DispatchAssignment.class))).thenAnswer(invocation -> {
            DispatchAssignment a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        DispatchAssignmentResponse response = assignmentService.assignCourier(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCourierId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(availableCourier.getCurrentActiveDeliveries()).isEqualTo(3);

        verify(courierRepository, times(1)).save(availableCourier);
        verify(assignmentRepository, times(1)).save(any(DispatchAssignment.class));
        verify(eventPublisherService, times(1)).publishCourierAssigned(any(CourierAssignedEvent.class));
    }

    @Test
    @DisplayName("Should throw CourierOverloadException when courier has reached capacity")
    void shouldThrowWhenCourierIsOverloaded() {
        AssignCourierRequest request = AssignCourierRequest.builder()
                .parcelId("p-123")
                .courierId(2L)
                .trackingNumber("SWIFT-001")
                .build();

        when(courierRepository.findById(2L)).thenReturn(Optional.of(fullCourier));

        assertThatThrownBy(() -> assignmentService.assignCourier(request))
                .isInstanceOf(CourierOverloadException.class)
                .hasMessageContaining("reached maximum active delivery limit");

        verify(assignmentRepository, never()).save(any(DispatchAssignment.class));
        verify(eventPublisherService, never()).publishCourierAssigned(any());
    }

    @Test
    @DisplayName("Should throw CourierUnavailableException when courier is offline")
    void shouldThrowWhenCourierIsOffline() {
        AssignCourierRequest request = AssignCourierRequest.builder()
                .parcelId("p-123")
                .courierId(3L)
                .trackingNumber("SWIFT-001")
                .build();

        when(courierRepository.findById(3L)).thenReturn(Optional.of(offlineCourier));

        assertThatThrownBy(() -> assignmentService.assignCourier(request))
                .isInstanceOf(CourierUnavailableException.class)
                .hasMessageContaining("currently OFFLINE");
    }

    @Test
    @DisplayName("Should throw InvalidAssignmentException when parcel is already actively assigned")
    void shouldThrowWhenParcelAlreadyAssigned() {
        AssignCourierRequest request = AssignCourierRequest.builder()
                .parcelId("p-123")
                .courierId(1L)
                .trackingNumber("SWIFT-001")
                .build();

        when(courierRepository.findById(1L)).thenReturn(Optional.of(availableCourier));
        when(assignmentRepository.findByParcelId("p-123")).thenReturn(Optional.of(sampleAssignment));

        assertThatThrownBy(() -> assignmentService.assignCourier(request))
                .isInstanceOf(InvalidAssignmentException.class)
                .hasMessageContaining("already assigned to a courier");
    }

    @Test
    @DisplayName("Should update assignment status and decrement courier load on completion")
    void shouldUpdateStatusAndDecrementOnCompletion() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(sampleAssignment));
        when(assignmentRepository.save(any(DispatchAssignment.class))).thenReturn(sampleAssignment);

        UpdateAssignmentStatusRequest request = UpdateAssignmentStatusRequest.builder()
                .status(AssignmentStatus.DELIVERED)
                .notes("Left with security guard")
                .build();

        int initialCourierLoad = availableCourier.getCurrentActiveDeliveries();

        DispatchAssignmentResponse response = assignmentService.updateAssignmentStatus(100L, request);

        assertThat(response).isNotNull();
        assertThat(sampleAssignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERED);
        assertThat(availableCourier.getCurrentActiveDeliveries()).isEqualTo(initialCourierLoad - 1);

        verify(courierRepository, times(1)).save(availableCourier);
        verify(eventPublisherService, times(1)).publishDeliveryStatusUpdated(any(DeliveryStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should throw InvalidAssignmentException when status is FAILED without failure reason")
    void shouldThrowWhenFailedWithoutReason() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(sampleAssignment));

        UpdateAssignmentStatusRequest request = UpdateAssignmentStatusRequest.builder()
                .status(AssignmentStatus.FAILED)
                .build();

        assertThatThrownBy(() -> assignmentService.updateAssignmentStatus(100L, request))
                .isInstanceOf(InvalidAssignmentException.class)
                .hasMessageContaining("Failure reason is mandatory");
    }

    @Test
    @DisplayName("Should successfully record Proof of Delivery")
    void shouldSubmitProofOfDelivery() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(sampleAssignment));
        when(proofOfDeliveryRepository.save(any(ProofOfDelivery.class))).thenAnswer(invocation -> {
            ProofOfDelivery p = invocation.getArgument(0);
            p.setId(50L);
            return p;
        });

        ProofOfDeliveryRequest request = ProofOfDeliveryRequest.builder()
                .recipientName("Kamal Gunaratne")
                .recipientNic("951234567V")
                .photoUrl("https://s3.swiftdrop.lk/pod/photo-100.jpg")
                .deliveryNotes("Received in person")
                .build();

        ProofOfDeliveryResponse response = assignmentService.submitProofOfDelivery(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getRecipientName()).isEqualTo("Kamal Gunaratne");
        assertThat(sampleAssignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERED);

        verify(proofOfDeliveryRepository, times(1)).save(any(ProofOfDelivery.class));
    }
}

