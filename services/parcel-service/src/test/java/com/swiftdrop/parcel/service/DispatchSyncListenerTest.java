package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.event.DispatchSyncListener;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.repository.ParcelStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DispatchSyncListenerTest {
    @Test void synchronizesDispatchAndIgnoresDuplicateAndStaleEvents() {
        var parcels=mock(ParcelRepository.class);var history=mock(ParcelStatusHistoryRepository.class);
        UUID id=UUID.randomUUID(),courier=UUID.randomUUID();
        var parcel=Parcel.builder().id(id).status(ParcelStatus.CREATED).build();
        when(parcels.findById(id)).thenReturn(Optional.of(parcel));
        var listener=new DispatchSyncListener(parcels,history);
        var event=new DispatchSyncListener.Event(id.toString(),courier.toString(),null,"IN_TRANSIT","Moving",null,LocalDateTime.of(2026,11,13,12,0));
        listener.receive(event);listener.receive(event);
        listener.receive(new DispatchSyncListener.Event(id.toString(),courier.toString(),"ASSIGNED",null,null,null,LocalDateTime.of(2026,11,13,11,0)));
        assertEquals(ParcelStatus.IN_TRANSIT,parcel.getStatus());assertEquals(courier,parcel.getAssignedCourierId());
        verify(parcels,times(1)).save(parcel);verify(history,times(1)).save(any());
    }
    @Test void neverReopensDeliveredParcels() {
        var parcels=mock(ParcelRepository.class);var history=mock(ParcelStatusHistoryRepository.class);
        UUID id=UUID.randomUUID();when(parcels.findById(id)).thenReturn(Optional.of(Parcel.builder().status(ParcelStatus.DELIVERED).build()));
        new DispatchSyncListener(parcels,history).receive(new DispatchSyncListener.Event(id.toString(),UUID.randomUUID().toString(),null,"IN_TRANSIT",null,null,LocalDateTime.now()));
        verify(parcels,never()).save(any());
    }
}
