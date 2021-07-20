package pl.allegro.tech.hermes.common.config;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.PropertyWrapper;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;

import javax.annotation.Nonnull;

/**
 * @author Cyril Wattebled (cyril.wattebled@idemia.com)
 */
public class KafkaClientConfig {
    public static final String KAFKA_CLIENT_PREFIX = "kafka.client.";

    public interface CollectionPut {
        Object put(String s, Object value);
    }

    public static void loadKakfaConfig(@Nonnull CollectionPut lambda) {
        ConfigDef config = new ConfigDef();
        SslConfigs.addClientSslSupport(config);
        SaslConfigs.addClientSaslSupport(config);
        DynamicPropertyFactory instance = DynamicPropertyFactory.getInstance();
        config.configKeys().forEach((s, configKey) -> {
            String propKey = KAFKA_CLIENT_PREFIX + s;
            PropertyWrapper<?> value = null;
            switch (configKey.type) {
                case BOOLEAN:
                    value = instance.getBooleanProperty(propKey, (Boolean) configKey.defaultValue);
                    break;
                case LIST:
                case CLASS:
                case PASSWORD:
                case STRING:
                    value = instance.getStringProperty(propKey, null);
                    break;
                case INT:
                case SHORT:
                    value = instance.getIntProperty(propKey, Short.toUnsignedInt((Short) configKey.defaultValue));
                    break;
                case LONG:
                    value = instance.getLongProperty(propKey, (Long) configKey.defaultValue);
                    break;
                case DOUBLE:
                    value = instance.getDoubleProperty(propKey, (Double) configKey.defaultValue);
                    break;
            }
            if (value.getValue() != null)
                lambda.put(s, value.getValue());
        });
    }
}
