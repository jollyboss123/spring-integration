package com.jolly.springintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
    }

    @Bean
    MessageChannel atob() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow flow() {
        return IntegrationFlow
                .from((MessageSource<String>) () ->
                        MessageBuilder.withPayload("Hello world " + Instant.now() + " !").build(),
                        poller -> poller.poller(pm -> pm.fixedRate(100)))
                .channel(atob())
                .get();
    }

    @Bean
    IntegrationFlow flow1() {
        return IntegrationFlow
                .from(atob())
                .transform((GenericTransformer<String, String>) source -> source.toUpperCase())
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("the payload is " + payload);
                    return null;
                })
                .get();
    }
}
