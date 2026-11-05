package com.swiftdrop.tracking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    TopicExchange dispatchExchange(@Value("${swiftdrop.tracking.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue trackingStatusQueue(@Value("${swiftdrop.tracking.queue}") String queue) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", "swiftdrop.tracking.dlx")
                .withArgument("x-dead-letter-routing-key", "tracking.rejected")
                .build();
    }

    @Bean
    Binding trackingStatusBinding(Queue trackingStatusQueue, TopicExchange dispatchExchange,
                                  @Value("${swiftdrop.tracking.routing-key}") String routingKey) {
        return BindingBuilder.bind(trackingStatusQueue).to(dispatchExchange).with(routingKey);
    }

    @Bean
    DirectExchange trackingDeadLetterExchange() {
        return new DirectExchange("swiftdrop.tracking.dlx", true, false);
    }

    @Bean
    Queue trackingDeadLetterQueue() {
        return QueueBuilder.durable("swiftdrop.tracking.rejected.queue").build();
    }

    @Bean
    Binding trackingDeadLetterBinding(Queue trackingDeadLetterQueue, DirectExchange trackingDeadLetterExchange) {
        return BindingBuilder.bind(trackingDeadLetterQueue).to(trackingDeadLetterExchange)
                .with("tracking.rejected");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
