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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
    }

    @Bean
    MessageChannel greetings() {
        return MessageChannels.direct().getObject();
    }

    private static String text() {
        return Math.random() > .5 ?
                "Hello " + Instant.now() + " !" :
                "Hola " + Instant.now() + " !";
    }

    /**
     * message source are like suppliers
     * that have to invoked i.e. polled/inquired
     * have to provide poller definition
     * by trigger, cron, fixed rate or delay
     */
    @Component
    static class CustomMessageSource implements MessageSource<String> {

        @Override
        public Message<String> receive() {
            return MessageBuilder.withPayload(text()).build();
        }
    }

    @Bean
    IntegrationFlow flow(CustomMessageSource customMessageSource) {
        return IntegrationFlow
                .from(customMessageSource,
                        sourcePollingChannelAdapterSpec -> sourcePollingChannelAdapterSpec.poller(
                                pollerFactory -> pollerFactory.fixedRate(Duration.ofSeconds(3))
                        ))
                .filter(String.class, source -> source.contains("Hola"))
                .transform((GenericTransformer<String, String>) String::toUpperCase)
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("the payload is " + payload);
                    return null;
                })
                .get();
    }
}
