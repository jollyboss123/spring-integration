package com.jolly.springintegration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * @author jolly
 */
//public class Sandbox {
//    static final String REQUESTS_CHANNEL = "requests";
//    static final String REPLIES_CHANNEL = "replies";
//
//    @Bean(name = REQUESTS_CHANNEL)
//    MessageChannel greetingsRequest() {
//        return MessageChannels.direct().getObject();
//    }
//
//    @Bean(name = REPLIES_CHANNEL)
//    DirectChannel greetingsReply() {
//        return MessageChannels.direct().getObject();
//    }
//
//    static String text() {
//        return Math.random() > .5 ?
//                "Hello " + Instant.now() + " !" :
//                "Hola " + Instant.now() + " !";
//    }
//
//    /**
//     * message source are like suppliers
//     * that have to invoked i.e. polled/inquired
//     * have to provide poller definition
//     * by trigger, cron, fixed rate or delay
//     */
//    @Component
//    static class CustomMessageSource implements MessageSource<String> {
//
//        @Override
//        public Message<String> receive() {
//            return MessageBuilder.withPayload(text()).build();
//        }
//    }
//
//    private static final String UPPERCASE_IN = "uin";
//    private static final String UPPERCASE_OUT = "uout";
//
//    /**
//     * component based handler
//     *
//     * @param id
//     * @param payload
//     * @return
//     */
//    @ServiceActivator(inputChannel = UPPERCASE_IN, outputChannel = UPPERCASE_OUT)
//    public String uppercase(@Header(MessageHeaders.ID) String id,
//                            @Payload String payload) {
//        System.out.println("the message id is: " + id);
//        return payload.toUpperCase();
//    }
//}
//
//@Component
//class Runner implements ApplicationRunner {
//    private final GreetingsClient greetingsClient;
//
//    Runner(GreetingsClient greetingsClient) {
//        this.greetingsClient = greetingsClient;
//    }
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
////        this.greetingsReply.subscribe(message ->
////                System.out.println("new message: "+ message.getPayload()));
////        for (int i = 0; i < 100; i++) {
////            this.greetingsClient.greet(SpringIntegrationApplication.text());
////        }
//        String reply = this.greetingsClient.greet("Bussing");
//        System.out.println("the reply: " + reply);
//    }
//}
//
///**
// * the problem with gateway is that it waits for a reply
// * when the message does not pass or fails to (in this case does not contain filter text "Hola"),
// * there is nothing that would produce a reply
// * thus, it's stuck there and is infinite by default
// */
//
//@MessagingGateway
//interface GreetingsClient {
//
//    @Gateway(requestChannel = Sandbox.REQUESTS_CHANNEL,
//            replyChannel = Sandbox.REPLIES_CHANNEL)
//    String greet(String text);
//}
