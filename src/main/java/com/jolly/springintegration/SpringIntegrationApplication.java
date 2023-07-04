package com.jolly.springintegration;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * spring integration is to ochestrate a pipeline
 */
@Log4j2
@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(SpringIntegrationApplication.class, args);
        Thread.currentThread().join();
    }

    private final Map<Integer, Order> orderDb = new ConcurrentHashMap<>();

    @Bean
    ApplicationRunner runner(MessageChannel orders) {
        return event -> {
            Order payload = new Order(1, Set.of(new LineItem("1"), new LineItem("2"), new LineItem("3")));
            this.orderDb.put(payload.id(), payload);
            Message<Order> orderMessage = MessageBuilder.withPayload(payload)
                    .setHeader("orderId", payload.id())
                    .build();
            orders.send(orderMessage);
        };
    }

    @Bean
    IntegrationFlow etailerFlow() {
        return IntegrationFlow
                .from(orders())
                .split(
                        (Function<Order, Collection<LineItem>>) Order::lineItems
//                        new AbstractMessageSplitter() {
//                    @Override
//                    protected Object splitMessage(Message<?> message) {
//                        Order order = (Order) message.getPayload();
//                        log.debug("------------");
//                        log.debug("received order: [{}]", order);
//                        return order.lineItems();
//                    }
//                }
                )
                .handle((GenericHandler<LineItem>) (payload, headers) -> {
                    log.debug("------------");
                    log.debug("received individual line item");
                    headers.forEach((key, value) -> log.debug("{} = {}", key, value));
                    return payload;
                })
                .aggregate()
                .handle((payload, headers) -> {
                    log.debug("orders after aggregation: [{}]", payload);
                    return null;
                })
                .get();
    }

    @Bean
    MessageChannel orders() {
        return MessageChannels.direct().getObject();
    }
}

record Order(Integer id, Set<LineItem> lineItems) {}

record LineItem(String sku) {}
