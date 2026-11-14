package com.swiftdrop.merchant.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    TopicExchange merchantDispatchExchange(@Value("${merchant.rabbitmq.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue merchantStatusQueue(@Value("${merchant.rabbitmq.queue}") String queue) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", "swiftdrop.merchant.dlx")
                .withArgument("x-dead-letter-routing-key", "merchant.status.rejected")
                .build();
    }

    @Bean
    Binding merchantStatusBinding(Queue merchantStatusQueue, TopicExchange merchantDispatchExchange,
                                  @Value("${merchant.rabbitmq.routing-key}") String routingKey) {
        return BindingBuilder.bind(merchantStatusQueue).to(merchantDispatchExchange).with(routingKey);
    }

    @Bean
    DirectExchange merchantDeadLetterExchange() {
        return new DirectExchange("swiftdrop.merchant.dlx", true, false);
    }

    @Bean
    Queue merchantDeadLetterQueue() {
        return QueueBuilder.durable("swiftdrop.merchant.rejected.queue").build();
    }

    @Bean
    Binding merchantDeadLetterBinding(Queue merchantDeadLetterQueue, DirectExchange merchantDeadLetterExchange) {
        return BindingBuilder.bind(merchantDeadLetterQueue).to(merchantDeadLetterExchange)
                .with("merchant.status.rejected");
    }

    @Bean
    MessageConverter merchantJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter merchantJsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(merchantJsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
