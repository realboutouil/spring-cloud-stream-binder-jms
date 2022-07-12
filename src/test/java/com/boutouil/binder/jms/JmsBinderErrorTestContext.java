package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableJms
public class JmsBinderErrorTestContext {

    @Bean
    public Sinks.Many<Message<String>> in() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<MessageHeaders> dlq() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender(Sinks.Many<Message<String>> in) {
        return in::asFlux;
    }

    @Bean
    public Consumer<Message<String>> consumer() {
        return message -> {
            log.info("Received message: {}", message);
            throw new RuntimeException("SENDING TO DLQ THIS EXCEPTION");
        };
    }

    @Bean
    public Consumer<Flux<Message<?>>> dlqConsumer(Sinks.Many<MessageHeaders> dlq) {
        return flux -> flux
                .subscribe(message -> {
                    log.info("Received message from DLQ: {}", message);
                    dlq.tryEmitNext(message.getHeaders())
                            .orThrow();
                });
    }
}
