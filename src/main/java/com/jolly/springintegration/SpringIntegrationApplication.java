package com.jolly.springintegration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(SpringIntegrationApplication.class, args);
        Thread.currentThread().join();
    }

    static final String REQUESTS_CHANNEL = "requests";
    static final String REPLIES_CHANNEL = "replies";

    @Bean(name = REQUESTS_CHANNEL)
    MessageChannel greetingsRequest() {
        return MessageChannels.direct().getObject();
    }

    @Bean(name = REPLIES_CHANNEL)
    DirectChannel greetingsReply() {
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
//                .filter(String.class, source -> source.contains("Hola"))
                .transform((GenericTransformer<String, String>) String::toUpperCase)
                .channel(greetingsReply())
                .get();
    }

//    @Bean
//    IntegrationFlow inboundFileSystemFlow() {
//        File directory = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/in"));
//        FileInboundChannelAdapterSpec files = Files.inboundAdapter(directory)
//                .autoCreateDirectory(true);
//        return IntegrationFlow
//                .from(files, poller -> poller.poller(pm -> PollerFactory.fixedRate(Duration.ofSeconds(1))))
//                .transform(new FileToStringTransformer()) // because greetingsRequest expects a String to filter
//                .handle((GenericHandler<String>) (payload, headers) -> {
//                    System.out.println("----- start of the line -----");
//                    headers.forEach((key, value) -> System.out.println(key + " = " + value));
//                    return payload;
//                })
//                .channel(greetingsRequest())
//                .get();
//    }
//
//    @Bean
//    IntegrationFlow outboundFileSystemFlow() {
//        File directory = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/out"));
//        return IntegrationFlow
//                .from(greetingsReply())
//                .handle((GenericHandler<String>) (payload, headers) -> {
//                    System.out.println("----- end of the line -----");
//                    headers.forEach((key, value) -> System.out.println(key + " = " + value));
//                    return payload;
//                })
//                .handle(Files.outboundAdapter(directory).autoCreateDirectory(true))
//                .get();
//    }
}

@Component
class Runner implements ApplicationRunner {
    private final GreetingsClient greetingsClient;

    Runner(GreetingsClient greetingsClient) {
        this.greetingsClient = greetingsClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        this.greetingsReply.subscribe(message ->
//                System.out.println("new message: "+ message.getPayload()));
//        for (int i = 0; i < 100; i++) {
//            this.greetingsClient.greet(SpringIntegrationApplication.text());
//        }
        String reply = this.greetingsClient.greet("Bussing");
        System.out.println("the reply: " + reply);
    }
}

/**
 * the problem with gateway is that it waits for a reply
 * when the message does not pass or fails to (in this case does not contain filter text "Hola"),
 * there is nothing that would produce a reply
 * thus, it's stuck there and is infinite by default
 */

@MessagingGateway
interface GreetingsClient {

    @Gateway(requestChannel = SpringIntegrationApplication.REQUESTS_CHANNEL,
    replyChannel = SpringIntegrationApplication.REPLIES_CHANNEL)
    String greet(String text);
}
