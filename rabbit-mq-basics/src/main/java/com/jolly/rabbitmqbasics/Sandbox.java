package com.jolly.rabbitmqbasics;

import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import java.util.Map;

/**
 * @author jolly
 */
@Log4j2
public class Sandbox {
    @Bean
    MessageChannel requests() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow rabbitProducerFlow(AmqpTemplate template) {
        return IntegrationFlow
                .from(this.requests())
                .handle(Amqp.outboundAdapter(template).routingKey(RabbitConfig.REQUEST_ROUTING_KEY).exchangeName(RabbitConfig.REQUEST_ROUTING_KEY))
                .get();
    }

    @Bean
    ApplicationRunner producer(MessageChannel requests) {
        return event -> {
            // payload could be any type as long as the consumer payload type matches it
            requests.send(MessageBuilder.withPayload(Map.of("message", "hello world")).build());
        };
    }

//    @Bean
//    ApplicationRunner producer(RabbitTemplate template) {
//        return event -> {
//            template.send(RabbitConfig.REQUEST_ROUTING_KEY, MessageBuilder.withBody("hello world".getBytes(StandardCharsets.UTF_8)).build());
//        };
//    }

    @Bean
    IntegrationFlow rabbitConsumerFlow(ConnectionFactory connectionFactory, Queue queue) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, queue).getObject())
                .handle((GenericHandler<Map<String, String>>) (payload, headers) -> {
                    headers.forEach((key, value) -> log.debug("{} = {}", key, value));
                    log.debug("got a new message: {}", payload);
                    return null;
                })
                .get();
    }

//    @RabbitListener(queues = RabbitConfig.REQUEST_ROUTING_KEY)
//    public void incomingRequests(@Payload String payload) {
//        log.debug("got a new message: {}", payload);
//    }
}

@Configuration
class RabbitConfig {
    public static final String REQUEST_ROUTING_KEY = "requests";

    @Bean
    Binding binding() {
        return BindingBuilder
                .bind(this.queue())
                .to(this.exchange())
                .with(REQUEST_ROUTING_KEY)
                .noargs();
    }

    @Bean
    Queue queue() {
        return QueueBuilder
                .durable(REQUEST_ROUTING_KEY)
                .build();
    }
    @Bean
    Exchange exchange() {
        return ExchangeBuilder
                .directExchange(REQUEST_ROUTING_KEY)
                .build();
    }
}
