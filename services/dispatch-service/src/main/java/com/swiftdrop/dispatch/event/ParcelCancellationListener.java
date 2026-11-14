package com.swiftdrop.dispatch.event;
import com.swiftdrop.dispatch.repository.DispatchAssignmentRepository;
import com.swiftdrop.dispatch.service.DispatchAssignmentService;
import com.swiftdrop.dispatch.dto.UpdateAssignmentStatusRequest;
import com.swiftdrop.dispatch.entity.AssignmentStatus;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Map;
@Component @RequiredArgsConstructor
public class ParcelCancellationListener {
 private final DispatchAssignmentRepository assignments;
 private final DispatchAssignmentService service;
 @RabbitListener(queues="swiftdrop.dispatch.parcel-cancellations")
 public void receive(Map<String,Object> event) {
  assignments.findByParcelId(event.get("parcelId").toString()).ifPresent(a -> {
   if(a.getStatus()==AssignmentStatus.ASSIGNED || a.getStatus()==AssignmentStatus.PICKED_UP || a.getStatus()==AssignmentStatus.IN_TRANSIT)
    service.updateAssignmentStatus(a.getId(),UpdateAssignmentStatusRequest.builder().status(AssignmentStatus.CANCELLED).notes("Cancelled by sender").build());
  });
 }
 @Configuration public static class Bindings {
  @Bean Queue cancellationQueue(){return QueueBuilder.durable("swiftdrop.dispatch.parcel-cancellations").build();}
  @Bean TopicExchange cancellationExchange(){return new TopicExchange("swiftdrop.parcel.exchange",true,false);}
  @Bean Binding cancellationBinding(){return BindingBuilder.bind(cancellationQueue()).to(cancellationExchange()).with("parcel.cancelled");}
 }
}
