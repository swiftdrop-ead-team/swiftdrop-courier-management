package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.CourierCreateRequest;
import com.swiftdrop.dispatch.dto.CourierResponse;
import com.swiftdrop.dispatch.dto.CourierStatusUpdateRequest;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;
import com.swiftdrop.dispatch.entity.Courier;
import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import com.swiftdrop.dispatch.exception.InvalidAssignmentException;
import com.swiftdrop.dispatch.exception.ResourceNotFoundException;
import com.swiftdrop.dispatch.repository.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private CourierRepository courierRepository;

    @InjectMocks
    private CourierServiceImpl courierService;

    private Courier sampleCourier;

    @BeforeEach
    void setUp() {
        sampleCourier = Courier.builder()
                .id(1L)
                .userId("courier-001")
                .fullName("Kasun Perera")
                .phoneNumber("+94771234567")
                .vehicleType(VehicleType.MOTORBIKE)
                .licensePlate("WP BDF-1029")
                .assignedZone("COLOMBO_CENTRAL")
                .status(CourierStatus.AVAILABLE)
                .maxActiveDeliveries(5)
                .currentActiveDeliveries(2)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new courier")
    void shouldRegisterCourierSuccessfully() {
        CourierCreateRequest request = CourierCreateRequest.builder()
                .userId("courier-new")
                .fullName("Sunil Shantha")
                .phoneNumber("+94778889900")
                .vehicleType(VehicleType.VAN)
                .licensePlate("WP CAS-1234")
                .assignedZone("COLOMBO_SOUTH")
                .maxActiveDeliveries(10)
                .build();

        when(courierRepository.findByUserId("courier-new")).thenReturn(Optional.empty());
        when(courierRepository.save(any(Courier.class))).thenAnswer(invocation -> {
            Courier c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        CourierResponse response = courierService.registerCourier(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getFullName()).isEqualTo("Sunil Shantha");
        assertThat(response.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        assertThat(response.getMaxActiveDeliveries()).isEqualTo(10);
        verify(courierRepository, times(1)).save(any(Courier.class));
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate courier userId")
    void shouldThrowWhenDuplicateCourierUserId() {
        CourierCreateRequest request = CourierCreateRequest.builder()
                .userId("courier-001")
                .fullName("Kasun Duplicate")
                .phoneNumber("+94771234567")
                .vehicleType(VehicleType.MOTORBIKE)
                .build();

        when(courierRepository.findByUserId("courier-001")).thenReturn(Optional.of(sampleCourier));

        assertThatThrownBy(() -> courierService.registerCourier(request))
                .isInstanceOf(InvalidAssignmentException.class)
                .hasMessageContaining("already exists");

        verify(courierRepository, never()).save(any(Courier.class));
    }

    @Test
    @DisplayName("Should retrieve courier by ID")
    void shouldGetCourierById() {
        when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

        CourierResponse response = courierService.getCourierById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Kasun Perera");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when courier ID does not exist")
    void shouldThrowWhenCourierNotFound() {
        when(courierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.getCourierById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Courier not found with id: 99");
    }

    @Test
    @DisplayName("Should calculate courier workloads accurately")
    void shouldCalculateCourierWorkloads() {
        when(courierRepository.findAll()).thenReturn(List.of(sampleCourier));

        List<CourierWorkloadResponse> workloads = courierService.getCourierWorkloads();

        assertThat(workloads).hasSize(1);
        CourierWorkloadResponse workload = workloads.get(0);
        assertThat(workload.getCourierId()).isEqualTo(1L);
        assertThat(workload.getActiveDeliveries()).isEqualTo(2);
        assertThat(workload.getMaxDeliveries()).isEqualTo(5);
        assertThat(workload.getUtilizationPercentage()).isEqualTo(40.0);
        assertThat(workload.getCanAcceptMore()).isTrue();
    }

    @Test
    @DisplayName("Should update courier status")
    void shouldUpdateCourierStatus() {
        when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
        when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

        CourierStatusUpdateRequest request = CourierStatusUpdateRequest.builder()
                .status(CourierStatus.OFFLINE)
                .build();

        CourierResponse response = courierService.updateCourierStatus(1L, request);

        assertThat(response).isNotNull();
        assertThat(sampleCourier.getStatus()).isEqualTo(CourierStatus.OFFLINE);
        verify(courierRepository, times(1)).save(sampleCourier);
    }
}

