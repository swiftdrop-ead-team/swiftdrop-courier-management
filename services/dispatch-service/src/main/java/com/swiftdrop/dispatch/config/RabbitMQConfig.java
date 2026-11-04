package com.swiftdrop.dispatch.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${swiftdrop.dispatch.exchange:dispatch.exchange}")
    private String dispatchExchange;

    @Value("${swiftdrop.dispatch.routing-key-assigned:dispatch.courier.assigned}")
    private String courierAssignedRoutingKey;

    @Value("${swiftdrop.dispatch.routing-key-status-updated:dispatch.status.updated}")
    private String statusUpdatedRoutingKey;

    @Bean
    public TopicExchange dispatchTopicExchange() {
        return new TopicExchange(dispatchExchange, true, false);
    }

    @Bean
    public Queue courierAssignedQueue() {
        return QueueBuilder.durable("swiftdrop.dispatch.courier-assigned.queue").build();
    }

    @Bean
    public Queue statusUpdatedQueue() {
        return QueueBuilder.durable("swiftdrop.dispatch.status-updated.queue").build();
    }

    @Bean
    public Binding bindingCourierAssigned(Queue courierAssignedQueue, TopicExchange dispatchTopicExchange) {
        return BindingBuilder.bind(courierAssignedQueue)
                .to(dispatchTopicExchange)
                .with(courierAssignedRoutingKey);
    }

    @Bean
    public Binding bindingStatusUpdated(Queue statusUpdatedQueue, TopicExchange dispatchTopicExchange) {
        return BindingBuilder.bind(statusUpdatedQueue)
                .to(dispatchTopicExchange)
                .with(statusUpdatedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

