package com.jolly.rabbitmqstreams;

import com.rabbitmq.stream.Environment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RabbitMqStreamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMqStreamsApplication.class, args);
    }

    @Bean
    InitializingBean initializingBean(RabbitProperties rabbitProperties, Environment environment) {
        return () -> environment.streamCreator().stream(rabbitProperties.getStream().getName()).create();
    }
}
