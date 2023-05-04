# spring-cloud-stream-binder-jms

A custom binder for Spring Cloud Stream with JMS support for JMS programs works with ActiveMQ and IBM MQ brokers.

Credit: [spring-cloud-stream-binder-jms](https://github.com/gevoulga/spring-cloud-stream-binder-jms)

## Introduction

This binder uses jakarta.jms-api 3.1.0 for the underlying binding of streams. To get started, simply add your dependency
for the underlying JMS implementation (e.g., using a Spring Boot starter), and you're good to go!

## Features

- Supports Topics & Queues
- Partitioning support
- Message delay support
- Dead-Letter-Queues: enabled by default with naming convention `<topicName>.dlq`

## Supported Versions

| Version       | Compatibility          |
|---------------|------------------------|
| 3.0.0.RELEASE | Works with 3.0.x       |
| 2.0.0.RELEASE | Works with 2.7.x/2.6.x |

## Dependencies

### Gradle

```groovy
implementation 'com.boutouil:spring-cloud-stream-binder-jms:3.0.0.RELEASE'
```

### Maven

```xml

<dependency>
    <groupId>com.boutouil</groupId>
    <artifactId>spring-cloud-stream-binder-jms</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>
```

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
@Import({ArtemisAutoConfiguration.class})
public class JmsBinderPartitionedTestContext {

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

    // An example with ActiveMQ
    // We are configuring the connection factory
    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer(Environment environment) {
        return factory -> factory.setClientID(String.join("-", environment.getActiveProfiles()));
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

## IBM MQ Example

For a sample project demonstrating the use of this Spring Cloud Stream binder with IBM MQ, please visit the following
repository:

[Spring Cloud Stream Binder MQ JMS Example](https://github.com/mohammedamineboutouil/spring-cloud-stream-binder-mq-jms)

## License

This project is licensed under the terms of the [LICENSE](LICENSE). Please refer to the LICENSE file in this repository
for more information.

## Contact Information

For any inquiries or support, please visit [www.boutouil.com/contact/](http://www.boutouil.com/contact/).
