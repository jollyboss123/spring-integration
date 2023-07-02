package com.jolly.springintegration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
    }

    @Bean
    MessageChannel greetingsRequest() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    MessageChannel greetingsResult() {
        return MessageChannels.direct().getObject();
    }

    static String text() {
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
    IntegrationFlow flow() {
        return IntegrationFlow
                .from(greetingsRequest())
                .filter(String.class, source -> source.contains("Hola"))
                .transform((GenericTransformer<String, String>) String::toUpperCase)
//                .handle((GenericHandler<String>) (payload, headers) -> {
//                    System.out.println("the payload is " + payload);
//                    return null;
//                })
                .channel(greetingsResult())
                .get();
    }
}

@Component
class Runner implements ApplicationRunner {
    private final GreetingsClient greetingsClient;

    Runner(GreetingsClient greetingsClient) {
        this.greetingsClient = greetingsClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println(this.greetingsClient.greet(SpringIntegrationApplication.text()));
        }
    }
}

@MessagingGateway
interface GreetingsClient {

    @Gateway(requestChannel = "greetingsRequest", replyChannel = "greetingsResult")
    String greet(String text);
}
