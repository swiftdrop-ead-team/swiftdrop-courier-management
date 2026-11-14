package com.swiftdrop.parcel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ParcelEstimateServiceTest {
    private ParcelEstimateService estimates;
    private final UUID owner = UUID.randomUUID();
    private CreateParcelRequest request;
    @BeforeEach void setup() {
        PricingClientService pricing = mock(PricingClientService.class);
        when(pricing.calculateDeliveryFee(any(),any(),any(),any(),any(),anyBoolean())).thenReturn(new BigDecimal("12.50"));
        var repository=mock(com.swiftdrop.parcel.repository.ParcelEstimateRepository.class);
        var records=new java.util.HashMap<UUID,com.swiftdrop.parcel.entity.ParcelEstimate>();
        when(repository.save(any())).thenAnswer(i -> {com.swiftdrop.parcel.entity.ParcelEstimate row=i.getArgument(0);records.put(row.getId(),row);return row;});
        when(repository.findById(any())).thenAnswer(i -> java.util.Optional.ofNullable(records.get(i.getArgument(0))));
        estimates = new ParcelEstimateService(pricing,new ObjectMapper(),repository);
        request = CreateParcelRequest.builder().recipientName("Recipient").deliveryAddress("18 Main Street").description("Books").build();
    }
    @Test void preservesAcceptedServerFeeAndPreventsReuse() {
        var quote=estimates.estimate(request,owner); request.setEstimateId(quote.id());
        assertEquals("LKR",quote.currency());
        assertEquals(quote.amount(),estimates.consume(request,owner));
        assertThrows(ResponseStatusException.class,()->estimates.consume(request,owner));
    }
    @Test void rejectsChangedBookingWithoutConsumingOriginalEstimate() {
        var quote=estimates.estimate(request,owner); request.setEstimateId(quote.id()); request.setDescription("Changed");
        assertThrows(ResponseStatusException.class,()->estimates.consume(request,owner));
        request.setDescription("Books"); assertEquals(quote.amount(),estimates.consume(request,owner));
    }
    @Test void rejectsAnotherAccount() {
        var quote=estimates.estimate(request,owner); request.setEstimateId(quote.id());
        assertThrows(ResponseStatusException.class,()->estimates.consume(request,UUID.randomUUID()));
        assertEquals(quote.amount(),estimates.consume(request,owner));
    }
}
