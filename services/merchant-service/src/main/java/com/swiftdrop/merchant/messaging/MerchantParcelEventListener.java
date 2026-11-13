package com.swiftdrop.merchant.messaging;

import com.swiftdrop.merchant.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.merchant.service.MerchantParcelEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantParcelEventListener {

    private final MerchantParcelEventService eventService;

    @RabbitListener(queues = "${merchant.rabbitmq.queue}",
            autoStartup = "${spring.rabbitmq.listener.simple.auto-startup:true}")
    public void onDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event, Message message) {
        eventService.process(event, message.getMessageProperties().getMessageId());
    }
}
