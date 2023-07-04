package com.jolly.basics;

import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.time.Duration;

/**
 * @author jolly
 */
public class Sandbox2 {
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
    IntegrationFlow errorFlow(MessageChannel errorChannel) {
        return IntegrationFlow
                .from(errorChannel)
                .handle((payload, headers) -> {
                    System.out.println("inside the errorFlow of: " + payload);
                    headers.forEach((k, v) -> System.out.println(k +  " = " + v));
                    return null;
                })
                .get();
    }

    @Bean
    IntegrationFlow errorForLetterViolationFlow(MessageChannel errorChannelForLetterViolation) {
        return IntegrationFlow
                .from(errorChannelForLetterViolation)
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
    IntegrationFlow inboundFileSystemFlow(MessageChannel uppercaseIn) {
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
                .channel(uppercaseIn)
                .get();
    }

    @Bean
    IntegrationFlow uppercaseFlow(MessageChannel uppercaseIn, MessageChannel errorChannelForLetterViolation, MessageChannel uppercaseOut) {
        return IntegrationFlow
                .from(uppercaseIn)
                .enrichHeaders(b -> b.errorChannel(errorChannelForLetterViolation)) // will override the default global error handling channel
                .handle((GenericHandler<String>) (payload, headers) -> {
                    for (char c : payload.toCharArray()) {
                        if (!Character.isLetter(c)) {
                            throw new IllegalArgumentException("Must be a character: " + c);
                        }
                    }
                    return payload;
                })
                .handle((GenericHandler<String>) (payload, headers) -> payload.toUpperCase())
                .channel(uppercaseOut)
                .get();
    }

    @Bean
    IntegrationFlow outboundFileSystemFlow(MessageChannel uppercaseOut) {
        File directory = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/out"));
        return IntegrationFlow
                .from(uppercaseOut)
                .handle((GenericHandler<String>) (payload, headers) -> {
                    System.out.println("----- end of the line -----");
                    headers.forEach((key, value) -> System.out.println(key + " = " + value));
                    return payload;
                })
                .handle(Files.outboundAdapter(directory).autoCreateDirectory(true))
                .get();
    }
}
