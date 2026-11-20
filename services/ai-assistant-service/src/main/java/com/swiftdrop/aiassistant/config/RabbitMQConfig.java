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

    @Bean public TopicExchange bookingExchange() { return new TopicExchange("swiftdrop.parcel.exchange", true, false); }
    @Bean public TopicExchange dispatchExchange() { return new TopicExchange("dispatch.exchange", true, false); }
    @Bean public Binding bookingReportsBinding(Queue reportsQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(reportsQueue).to(bookingExchange).with("parcel.created");
    }
    @Bean public Binding dispatchReportsBinding(Queue reportsQueue, TopicExchange dispatchExchange) {
        return BindingBuilder.bind(reportsQueue).to(dispatchExchange).with("dispatch.status.updated");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}