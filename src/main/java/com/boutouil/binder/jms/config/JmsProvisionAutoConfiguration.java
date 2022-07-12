package com.boutouil.binder.jms.config;

import com.boutouil.binder.jms.provision.JmsProvisioner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

@Configuration
@AutoConfigureAfter(JndiConnectionFactoryAutoConfiguration.class)
@ConditionalOnClass(ConnectionFactory.class)
public class JmsProvisionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JmsProvisioner jmsProvisioner(ConnectionFactory connectionFactory) {
        return new JmsProvisioner(connectionFactory);
    }
}
