package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableJms
public class JmsMessageChannelBinderTestContext {

    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
        return factory -> {
            factory.setClientID("clientId");
        };
    }

    @Bean
    public Sinks.Many<Message<String>> senderSink() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender(Sinks.Many<Message<String>> senderSink) {
        return senderSink::asFlux;
    }

    @Bean
    public Function<Flux<Message<String>>, Mono<Void>> consumer() {
        return flux -> flux
                .doOnEach(message -> log.info("Received message: {}", message))
                .then();
    }

    @Bean
    public Function<Flux<Message<String>>, Flux<Message<String>>> groupper() {
        return data -> data
                .window(Duration.ofSeconds(3))
                .flatMap(messageFlux -> messageFlux.collectList()
                        .map(messages -> {
                            String str = messages.stream()
                                    .map(Message::getPayload)
                                    .collect(Collectors.joining(","));
                            log.info("Within 3s window we received: {}", str);
                            return MessageBuilder.withPayload(str).build();
                        })
                );
    }
}
