package com.jolly.springintegration;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
    }

    @Bean
    MessageChannel greetings() {
        return MessageChannels.direct().getObject();
    }

    private String text() {
        return Math.random() > .5 ?
                "Hello " + Instant.now() + " !" :
                "Hola " + Instant.now() + " !";
    }

    @Bean
    IntegrationFlow flow() {
        return IntegrationFlow
                .from((MessageSource<String>) () ->
                        MessageBuilder.withPayload(text()).build(),
                        poller -> poller.poller(pm -> pm.fixedRate(100)))
                .filter(String.class, source -> source.contains("Hola"))
                .channel(greetings())
                .get();
    }

    @Bean
    IntegrationFlow flow1() {
        return IntegrationFlow
                .from(greetings())
                .transform((GenericTransformer<String, String>) String::toUpperCase)
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("the payload is " + payload);
                    return null;
                })
                .get();
    }
}
