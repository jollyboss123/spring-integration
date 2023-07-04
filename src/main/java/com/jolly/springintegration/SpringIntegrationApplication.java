package com.jolly.springintegration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.annotation.ServiceActivator;
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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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

    /**
     * by default when face error, will automatically look for a errorChannel bean
     * and follow that bean's pipeline
     * this is our global default error channel
     *
     * @return
     */
    @Bean
    MessageChannel errorChannel() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    MessageChannel errorChannelForLetterViolation() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow errorFlow() {
        return IntegrationFlow
                .from(errorChannel())
                .handle((payload, headers) -> {
                    System.out.println("inside the errorFlow of: " + payload);
                    headers.forEach((k, v) -> System.out.println(k +  " = " + v));
                    return null;
                })
                .get();
    }

    @Bean
    IntegrationFlow errorForLetterViolationFlow() {
        return IntegrationFlow
                .from(errorChannelForLetterViolation())
                .handle((payload, headers) -> {
                    System.out.println("inside the errorFlowForLetterViolation of: " + payload);
                    headers.forEach((k, v) -> System.out.println(k +  " = " + v));
                    return null;
                })
                .get();
    }

    @Bean
    MessageChannel uppercaseIn() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    MessageChannel uppercaseOut() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow inboundFileSystemFlow() {
        File directory = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/in"));
        FileInboundChannelAdapterSpec files = Files.inboundAdapter(directory)
                .autoCreateDirectory(true);
        return IntegrationFlow
                .from(files, poller -> poller.poller(pm -> PollerFactory.fixedRate(Duration.ofSeconds(2))))
                .transform(new FileToStringTransformer())
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("----- start of the line -----");
                    headers.forEach((key, value) -> System.out.println(key + " = " + value));
                    return payload;
                })
                .channel(uppercaseIn())
                .get();
    }

    @Bean
    IntegrationFlow uppercaseFlow() {
        return IntegrationFlow
                .from(uppercaseIn())
                .enrichHeaders(b -> b.errorChannel(errorChannelForLetterViolation())) // will override the default global error handling channel
                .handle((GenericHandler<String>) (payload, headers) -> {
                    for (char c : payload.toCharArray()) {
                        if (!Character.isLetter(c)) {
                            throw new IllegalArgumentException("Must be a character: " + c);
                        }
                    }
                    return payload;
                })
                .handle((GenericHandler<String>) (payload, headers) -> payload.toUpperCase())
                .channel(uppercaseOut())
                .get();
    }

    @Bean
    IntegrationFlow outboundFileSystemFlow() {
        File directory = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/out"));
        return IntegrationFlow
                .from(uppercaseOut())
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("----- end of the line -----");
                    headers.forEach((key, value) -> System.out.println(key + " = " + value));
                    return payload;
                })
                .handle(Files.outboundAdapter(directory).autoCreateDirectory(true))
                .get();
    }
}
