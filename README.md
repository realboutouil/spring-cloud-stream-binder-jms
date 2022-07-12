# spring-cloud-stream-binder-jms

A binder for spring cloud stream using JMS

## Introduction

Binder using javax.jms-api 2.0.1 for the underlying binding of streams.  
Simply add your dependency of the underlying implementation of JMS (using a spring-boot-stater?), and that's it!

## Features

- Topics & Queues supported
- Partitioning
- Delay
- Dead-Letter-Queues: by default enabled -> <topicName>.dlq

## Examples

### Topic

Configuration:

```yaml
spring:
  cloud:
    stream:
      function.definition: sender;consumer
      binder.jms.type: jms
      bindings:
        sender-out-0:
          #change this to queue://ticks if you want queues!
          destination: topic://ticks
        consumer-in-0:
          #change this to queue://ticks if you want queues!
          destination: topic://ticks
      jms.bindings:
        consumer-in-0:
          consumer:
            shared: false
            durable: false
```

Beans:

```java

@SpringBootApplication
@EnableJms
public class JmsBinderPartitionedTestContext {

    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer(
            Environment environment
    ) {
        return factory -> {
            factory.setClientID(String.join("-", environment.getActiveProfiles()));
        };
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender() {
        //Your publisher to the stream
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer() {
        //The consumer of the stream
    }
}
```

### Partitioned

Configuration:

```yaml
spring:
  cloud:
    stream:
      function.definition: sender;consumer0;consumer1
      binder.jms.type: jms
      bindings:
        sender-out-0:
          destination: topic://ticks
          producer:
            partition-key-expression: new Integer(payload)
            partition-count: 2
        consumer0-in-0:
          destination: topic://ticks-0
          group: consumer-group
        consumer1-in-0:
          destination: topic://ticks-1
          group: consumer-group
      #Custom JMS consumer properties
      jms:
        default:
          consumer:
            shared: false
            durable: false
            #Dead letter queue configuration
            #dlq: .dlq -> <topic>.dlq
            #dlq: dlq. -> dlq.<topic>
            #dlq: dlq -> explicit dlq topic
            #dlq: "" -> dlq is disabled
```

Beans:

```java

@SpringBootApplication
@EnableJms
public class JmsBinderPartitionedTestContext {

    //An example with ActiveMQ
    //We are configuring the connection factory
    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer(
            Environment environment
    ) {
        return factory -> {
            factory.setClientID(String.join("-", environment.getActiveProfiles()));
        };
    }

    @Bean
    public Supplier<Flux<Message<String>>> sender() {
        //Your publisher to the stream
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer0() {
        //The first consumer of the stream
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumer1(Sinks.Many<String> out1) {
        //The second consumer of the stream
    }
}
```
