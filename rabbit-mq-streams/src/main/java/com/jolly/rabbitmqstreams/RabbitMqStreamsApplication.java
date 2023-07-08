package com.jolly.rabbitmqstreams;

import com.rabbitmq.stream.Environment;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.RabbitStream;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;

import java.util.Map;

@SpringBootApplication
public class RabbitMqStreamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMqStreamsApplication.class, args);
    }

    static Map<String, String> payload(String name) {
        return Map.of("message", "Hello " + name);
    }
}

@Configuration
@Log4j2
class RabbitMqStreamsConfiguration {
    @Bean
    MessageChannel streamMessageChannel() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    IntegrationFlow inboundRabbitStreamIntegrationFlow(RabbitProperties rabbitProperties,
                                                       Environment environment) {
        return IntegrationFlow
                .from(RabbitStream.inboundAdapter(environment)
                        .configureContainer(container -> container.queueName(rabbitProperties.getStream().getName())))
                .handle((payload, headers) -> {
                    log.debug("got the stream payload: {}", payload);
                    return null;
                })
                .get();
    }

    @Bean
    InitializingBean initializingBean(RabbitProperties rabbitProperties, Environment environment) {
        return () -> environment.streamCreator().stream(rabbitProperties.getStream().getName()).create();
    }

    @Bean
    ApplicationRunner rabbitMqStreamInitialized() {
        return args -> this.streamMessageChannel()
                .send(MessageBuilder.withPayload(
                        RabbitMqStreamsApplication.payload("Integration stream")).build());
    }

    @Bean
    IntegrationFlow outboundRabbitStreamIntegrationFlow(RabbitStreamTemplate template) {
        return IntegrationFlow
                .from(this.streamMessageChannel())
                .handle(RabbitStream.outboundStreamAdapter(template))
                .get();
    }
}
