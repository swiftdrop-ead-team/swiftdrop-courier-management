package com.swiftdrop.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message broker configuration for Notification Service.
 * Declares the topic exchange, notification queues, bindings, and JSON message converter.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${swiftdrop.notification.rabbitmq.exchange:dispatch.exchange}")
    private String exchangeName;

    @Value("${swiftdrop.notification.rabbitmq.queue-status-updated:swiftdrop.notification.status-updated.queue}")
    private String statusUpdatedQueueName;

    @Value("${swiftdrop.notification.rabbitmq.queue-courier-assigned:swiftdrop.notification.courier-assigned.queue}")
    private String courierAssignedQueueName;

    @Value("${swiftdrop.notification.rabbitmq.routing-key-status-updated:dispatch.status.updated}")
    private String statusUpdatedRoutingKey;

    @Value("${swiftdrop.notification.rabbitmq.routing-key-assigned:dispatch.courier.assigned}")
    private String courierAssignedRoutingKey;

    /**
     * Declares the Topic Exchange (dispatch.exchange).
     * Durable = true so exchange survives broker restarts.
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Durable queue for delivery status update events.
     */
    @Bean
    public Queue statusUpdatedQueue() {
        return QueueBuilder.durable(statusUpdatedQueueName).build();
    }

    /**
     * Durable queue for courier assignment events.
     */
    @Bean
    public Queue courierAssignedQueue() {
        return QueueBuilder.durable(courierAssignedQueueName).build();
    }

    /**
     * Binds the status updated queue to the exchange using the routing key.
     */
    @Bean
    public Binding bindingStatusUpdated(Queue statusUpdatedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(statusUpdatedQueue)
                .to(notificationExchange)
                .with(statusUpdatedRoutingKey);
    }

    /**
     * Binds the courier assigned queue to the exchange using the routing key.
     */
    @Bean
    public Binding bindingCourierAssigned(Queue courierAssignedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(courierAssignedQueue)
                .to(notificationExchange)
                .with(courierAssignedRoutingKey);
    }

    /**
     * JSON Message Converter to automatically deserialize incoming RabbitMQ JSON into Java DTOs.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configured with the JSON message converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

