package com.boutouil.binder.jms.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

@Getter
@Setter
public class JmsBindingProperties implements BinderSpecificPropertiesProvider {

    private JmsProducerProperties producer = new JmsProducerProperties();

    private JmsConsumerProperties consumer = new JmsConsumerProperties();
}
