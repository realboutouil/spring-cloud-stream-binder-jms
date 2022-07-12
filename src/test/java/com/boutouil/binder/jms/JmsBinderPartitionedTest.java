package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

@Isolated
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = JmsBinderPartitionedTestContext.class)
@ActiveProfiles("partitioned")
public class JmsBinderPartitionedTest {

    @Autowired
    Sinks.Many<Message<String>> in;
    @Autowired
    Sinks.Many<String> out0;
    @Autowired
    Sinks.Many<String> out1;
    @Autowired
    Sinks.Many<String> out2;

    @Test
    void test() {
        Flux.interval(Duration.ofSeconds(1))
                .take(9)
                .subscribe(aLong -> {
                    Message<String> message = MessageBuilder
                            .withPayload(aLong.toString())
                            .build();
                    log.info("Sending message: {}", message);
                    in.tryEmitNext(message).orThrow();
                });

        StepVerifier
                .create(out0.asFlux())
                .expectNext("0")
                .expectNext("3")
                .expectNext("6")
                .thenCancel()
                .verify(Duration.ofSeconds(12));

        StepVerifier
                .create(out1.asFlux())
                .expectNext("1")
                .expectNext("4")
                .expectNext("7")
                .thenCancel()
                .verify(Duration.ofSeconds(12));

        StepVerifier
                .create(out2.asFlux())
                .expectNext("2")
                .expectNext("5")
                .expectNext("8")
                .thenCancel()
                .verify(Duration.ofSeconds(12));
    }
}
