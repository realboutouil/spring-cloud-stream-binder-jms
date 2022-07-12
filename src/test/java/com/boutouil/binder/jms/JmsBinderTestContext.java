package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableJms
public class JmsBinderTestContext {

    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer(
            Environment environment
    ) {
        return factory -> {
            factory.setClientID(String.join("-", environment.getActiveProfiles()));
        };
    }

    @Bean
    public Sinks.Many<Message<String>> in() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<String> out() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender(Sinks.Many<Message<String>> in) {
        return in::asFlux;
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer(Sinks.Many<String> out) {
        return flux -> flux
                .subscribe(message -> {
                    log.info("Received message: {}", message);
                    out.tryEmitNext(message.getPayload())
                            .orThrow();
                });
    }
}
