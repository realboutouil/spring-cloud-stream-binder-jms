package com.boutouil.binder.jms;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Isolated
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = JmsBinderErrorTestContext.class)
@ActiveProfiles("dlq")
public class JmsBinderErrorTest {

    @Autowired
    Sinks.Many<Message<String>> in;
    @Autowired
    Sinks.Many<MessageHeaders> dlq;

    @Test
    void test() {
        Flux.interval(Duration.ofSeconds(1))
                .take(1)
                .subscribe(aLong -> {
                    Message<String> message = MessageBuilder
                            .withPayload(aLong.toString())
                            .build();
                    log.info("Sending message: {}", message);
                    in.tryEmitNext(message).orThrow();
                });

        StepVerifier
                .create(dlq.asFlux())
                .expectNextMatches(headers -> {
                    assertThat(headers)
                            .containsEntry("x_exception_message", "SENDING TO DLQ THIS EXCEPTION")
                            .containsEntry("x_original_destination", ActiveMQQueue.createQueue("dlt-ticks")
                                    .toString());
                    return true;
                })
                .thenCancel()
                .verify(Duration.ofSeconds(1500));
    }
}
