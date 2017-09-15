package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.ssl.JvmKeystoreSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;

import javax.inject.Inject;
import java.util.Optional;

public class SslContextFactoryProvider {

    @Inject
    @org.jvnet.hk2.annotations.Optional
    SslContextFactory sslContextFactory;

    @Inject
    ConfigFactory configFactory;

    public SslContextFactory getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElseGet(this::getDefault);
    }

    private SslContextFactory getDefault() {
        String protocol = configFactory.getStringProperty(Configs.CONSUMER_SSL_PROTOCOL);
        KeystoreProperties keystoreProperties = new KeystoreProperties(
                configFactory.getStringProperty(Configs.CONSUMER_SSL_KEYSTORE_LOCATION),
                configFactory.getStringProperty(Configs.CONSUMER_SSL_KEYSTORE_FORMAT),
                configFactory.getStringProperty(Configs.CONSUMER_SSL_KEYSTORE_PASSWORD));
        KeystoreProperties truststoreProperties = new KeystoreProperties(
                configFactory.getStringProperty(Configs.CONSUMER_SSL_TRUSTSTORE_LOCATION),
                configFactory.getStringProperty(Configs.CONSUMER_SSL_TRUSTSTORE_FORMAT),
                configFactory.getStringProperty(Configs.CONSUMER_SSL_TRUSTSTORE_PASSWORD));
        return new JvmKeystoreSslContextFactory(protocol, keystoreProperties, truststoreProperties);
    }
}
