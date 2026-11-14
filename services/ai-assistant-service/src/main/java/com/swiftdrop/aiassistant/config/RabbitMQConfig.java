package com.swiftdrop.aiassistant.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "parcel.exchange";
    public static final String REPORTS_QUEUE = "reports.parcel.status.queue";
    public static final String ROUTING_KEY = "parcel.status.changed";

    @Bean
    public TopicExchange parcelExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue reportsQueue() {
        return new Queue(REPORTS_QUEUE, true);
    }

    @Bean
    public Binding reportsBinding(Queue reportsQueue, TopicExchange parcelExchange) {
        return BindingBuilder.bind(reportsQueue).to(parcelExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}