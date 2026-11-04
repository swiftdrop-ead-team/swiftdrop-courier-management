package com.swiftdrop.dispatch.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message broker configuration.
 * Sets up the topic exchange, queues, routing keys, and JSON message converter.
 * Enables asynchronous event communication with other microservices (e.g., Notification Service).
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name for dispatch events (default: dispatch.exchange)
    @Value("${swiftdrop.dispatch.exchange:dispatch.exchange}")
    private String dispatchExchange;

    // Routing key used when a courier is assigned to a parcel
    @Value("${swiftdrop.dispatch.routing-key-assigned:dispatch.courier.assigned}")
    private String courierAssignedRoutingKey;

    // Routing key used when a delivery status updates (in-transit, delivered, failed)
    @Value("${swiftdrop.dispatch.routing-key-status-updated:dispatch.status.updated}")
    private String statusUpdatedRoutingKey;

    /**
     * Declares the Topic Exchange for dispatch events.
     * Durable = true means the exchange survives broker restart.
     */
    @Bean
    public TopicExchange dispatchTopicExchange() {
        return new TopicExchange(dispatchExchange, true, false);
    }

    /**
     * Queue to store events when a courier is assigned.
     * Consumed by services like Notification Service to send SMS/Email to courier.
     */
    @Bean
    public Queue courierAssignedQueue() {
        return QueueBuilder.durable("swiftdrop.dispatch.courier-assigned.queue").build();
    }

    /**
     * Queue to store events when delivery status changes.
     */
    @Bean
    public Queue statusUpdatedQueue() {
        return QueueBuilder.durable("swiftdrop.dispatch.status-updated.queue").build();
    }

    /**
     * Binds the courier assigned queue to the dispatch exchange using the routing key.
     */
    @Bean
    public Binding bindingCourierAssigned(Queue courierAssignedQueue, TopicExchange dispatchTopicExchange) {
        return BindingBuilder.bind(courierAssignedQueue)
                .to(dispatchTopicExchange)
                .with(courierAssignedRoutingKey);
    }

    /**
     * Binds the status updated queue to the dispatch exchange using the routing key.
     */
    @Bean
    public Binding bindingStatusUpdated(Queue statusUpdatedQueue, TopicExchange dispatchTopicExchange) {
        return BindingBuilder.bind(statusUpdatedQueue)
                .to(dispatchTopicExchange)
                .with(statusUpdatedRoutingKey);
    }

    /**
     * Message converter to serialize Java event objects into JSON before sending to RabbitMQ.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures RabbitTemplate with JSON message converter for sending messages.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
