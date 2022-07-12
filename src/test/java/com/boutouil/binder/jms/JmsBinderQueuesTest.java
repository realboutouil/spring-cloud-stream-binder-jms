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
@ContextConfiguration(classes = JmsBinderTestContext.class)
@ActiveProfiles("queues")
public class JmsBinderQueuesTest {

    @Autowired
    Sinks.Many<Message<String>> in;
    @Autowired
    Sinks.Many<String> out;

    @Test
    void test() {
        Flux.interval(Duration.ofSeconds(1))
                .take(3)
                .subscribe(aLong -> {
                    Message<String> message = MessageBuilder
                            .withPayload(aLong.toString())
                            .build();
                    log.info("Sending message: {}", message);
                    in.tryEmitNext(message).orThrow();
                });

        StepVerifier
                .create(out.asFlux())
                .expectNext("0")
                .expectNext("1")
                .expectNext("2")
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }
}
