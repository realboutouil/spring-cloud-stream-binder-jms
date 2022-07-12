package com.boutouil.binder.jms.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.AbstractExtendedBindingProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

import java.util.Map;

@ConfigurationProperties("spring.cloud.stream.jms")
public class JmsExtendedBindingProperties extends AbstractExtendedBindingProperties
        <JmsConsumerProperties, JmsProducerProperties, JmsBindingProperties> {

    public static final String DEFAULTS_PREFIX = "spring.cloud.stream.jms.default";

    @Override
    public String getDefaultsPrefix() {
        return DEFAULTS_PREFIX;
    }

    @Override
    public Map<String, ?> getBindings() {
        return doGetBindings();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return JmsBindingProperties.class;
    }
}
