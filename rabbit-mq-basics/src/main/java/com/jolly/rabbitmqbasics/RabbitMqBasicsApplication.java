package com.jolly.rabbitmqbasics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.jolly.rabbitmqbasics.RabbitMqBasicsApplication.*;

@Log4j2
@SpringBootApplication
public class RabbitMqBasicsApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(RabbitMqBasicsApplication.class, args);
        Thread.currentThread().join();
    }

    static final String INTEGRATION_REQUESTS_NAME = "integration-requests";
    static final String BASIC_REQUESTS_NAME = "basic-requests";

    static Map<String, String> payload(String name) {
        return Map.of("message", "Hello " + name);
    }

    static void dump(Object payload, Map<String, Object> headers) {
        log.debug("-----------------");
        headers.forEach((k, v) -> log.debug("{} = {}", k, v));
        log.debug(payload);
    }
}

@Configuration
class RabbitConfiguration {
    private final ObjectMapper objectMapper;
    private final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
    };

    RabbitConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    ApplicationRunner basicsProducer(AmqpTemplate template) {
        return args -> {
            var json = objectMapper.writeValueAsBytes(payload("Basics"));
            var message = MessageBuilder
                    .withBody(json)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            template.send(BASIC_REQUESTS_NAME, BASIC_REQUESTS_NAME, message);
        };
    }

    @RabbitListener(queues = BASIC_REQUESTS_NAME)
    public void basicsConsumer(@Headers Map<String,Object> headers, @Payload Message payload) throws IOException {
        var map = this.objectMapper.readValue(payload.getBody(), this.typeReference);
        dump(map, headers);
    }
}

@Configuration
class RabbitIntegrationConfiguration {
    @Bean
    IntegrationFlow inboundRabbitIntegrationFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, INTEGRATION_REQUESTS_NAME))
                .handle((payload, headers) -> {
                    dump(payload, headers);
                    return null;
                })
                .get();
    }

    @Bean
    MessageChannel integrationMessageChannel() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow outboundRabbitIntegrationFlow(AmqpTemplate template) {
        return IntegrationFlow
                .from(this.integrationMessageChannel())
                .handle(Amqp.outboundAdapter(template)
                        .exchangeName(INTEGRATION_REQUESTS_NAME)
                        .routingKey(INTEGRATION_REQUESTS_NAME)
                        .getObject())
                .get();
    }

    @Bean
    ApplicationRunner integrationRunner() {
        return args -> {
            integrationMessageChannel()
                    .send(org.springframework.messaging.support.MessageBuilder.withPayload(
                            payload("Integration")
                    ).build());
        };
    }
}

@Configuration
class InfrastructureConfiguration {
    @Bean
    InitializingBean initializeRabbitMqBroker(AmqpAdmin admin) {
        return () -> Set
                .of(INTEGRATION_REQUESTS_NAME, BASIC_REQUESTS_NAME)
                .forEach(name -> define(admin, name));
    }

    private static Queue define(AmqpAdmin admin, String name) {
        var q = QueueBuilder
                .durable(name)
                .build();
        var e = ExchangeBuilder
                .topicExchange(name)
                .build();
        var b = BindingBuilder
                .bind(q)
                .to(e)
                .with(name)
                .noargs();
        admin.declareQueue(q);
        admin.declareExchange(e);
        admin.declareBinding(b);
        return q;
    }
}


