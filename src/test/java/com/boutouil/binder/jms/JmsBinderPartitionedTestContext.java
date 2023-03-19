package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import({ArtemisAutoConfiguration.class})
public class JmsBinderPartitionedTestContext {

    @Bean
    public Sinks.Many<Message<String>> in() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<String> out0() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<String> out1() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<String> out2() {
        return Sinks.many().unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender(Sinks.Many<Message<String>> in) {
        return in::asFlux;
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer0(Sinks.Many<String> out0) {
        return flux -> flux
                .subscribe(message -> {
                    log.info("Received on Partition0 message: {}", message);
                    out0.tryEmitNext(message.getPayload())
                            .orThrow();
                });
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer1(Sinks.Many<String> out1) {
        return flux -> flux
                .subscribe(message -> {
                    log.info("Received on Partition1 message: {}", message);
                    out1.tryEmitNext(message.getPayload())
                            .orThrow();
                });
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer2(Sinks.Many<String> out2) {
        return flux -> flux
                .subscribe(message -> {
                    log.info("Received on Partition2 message: {}", message);
                    out2.tryEmitNext(message.getPayload())
                            .orThrow();
                });
    }
}
