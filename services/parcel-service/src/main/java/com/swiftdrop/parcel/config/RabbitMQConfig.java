package com.swiftdrop.parcel.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitMQConfig {

    @Value("${swiftdrop.rabbitmq.exchange.parcel:swiftdrop.parcel.exchange}")
    private String parcelExchangeName;

    @Value("${swiftdrop.rabbitmq.queues.tracking:swiftdrop.tracking.parcel-updates}")
    private String trackingQueueName;

    @Value("${swiftdrop.rabbitmq.queues.notification:swiftdrop.notification.parcel-updates}")
    private String notificationQueueName;

    @Bean
    public TopicExchange parcelExchange() {
        return new TopicExchange(parcelExchangeName, true, false);
    }

    @Bean
    public Queue trackingQueue() {
        return QueueBuilder.durable(trackingQueueName).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueueName).build();
    }

    @Bean
    public Binding trackingBinding(Queue trackingQueue, TopicExchange parcelExchange) {
        return BindingBuilder.bind(trackingQueue)
                .to(parcelExchange)
                .with("parcel.#");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange parcelExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(parcelExchange)
                .with("parcel.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

