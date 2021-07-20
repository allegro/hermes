package pl.allegro.tech.hermes.common.config;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.Assert;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Cyril Wattebled (cyril.wattebled@idemia.com)
 */
public class KafkaClientConfigTest {

    /**
     * Read from config.properties (in test-resources) and loads the kafka client related configuration keys
     */
    @Test
    public void loadKakfaConfig() {
        Map<String, Object> map = new HashMap<>();
        KafkaClientConfig.loadKakfaConfig(map::putIfAbsent);
        Assert.assertEquals("file:/./truststore.jks", map.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        Assert.assertEquals("software.amazon.msk.auth.iam.IAMLoginModule required;", map.get(SaslConfigs.SASL_JAAS_CONFIG));
        Assert.assertEquals("AWS_MSK_IAM", map.get(SaslConfigs.SASL_MECHANISM));
        Assert.assertEquals("software.amazon.msk.auth.iam.IAMClientCallbackHandler", map.get(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS));
    }
}