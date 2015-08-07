package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.function.Supplier;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SSLContextSupplier implements Supplier<SSLContext> {
    private final String protocol;
    private final KeystoreProperties keyStoreProperties;
    private final KeystoreProperties trustStoreProperties;

    public SSLContextSupplier(ConfigFactory configFactory) {
        this(configFactory.getStringProperty(Configs.FRONTEND_SSL_PROTOCOL),
                new KeystoreProperties(
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_KEYSTORE_LOCATION),
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_KEYSTORE_FORMAT),
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_KEYSTORE_PASSWORD)),
                new KeystoreProperties(
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_TRUSTSTORE_LOCATION),
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_TRUSTSTORE_FORMAT),
                        configFactory.getStringProperty(Configs.FRONTEND_SSL_TRUSTSTORE_PASSWORD)));
    }

    public SSLContextSupplier(String protocol, KeystoreProperties keyStoreProperties, KeystoreProperties trustStoreProperties) {
        this.protocol = protocol;
        this.keyStoreProperties = keyStoreProperties;
        this.trustStoreProperties = trustStoreProperties;
    }

    @Override
    public SSLContext get() {
        try {
            return createSSLContext(loadKeyStore(keyStoreProperties), loadKeyStore(trustStoreProperties));
        } catch (Exception e) {
            throw new IllegalStateException("Something went wrong with setting up SSL context.", e);
        }
    }

    private KeyStore loadKeyStore(KeystoreProperties props) throws Exception {
        try (InputStream stream = getResourceAsInputStream(props.getLocationAsURI())) {
            KeyStore loadedKeystore = KeyStore.getInstance(props.getFormat());
            loadedKeystore.load(stream, props.getPassword().toCharArray());
            return loadedKeystore;
        }
    }

    private InputStream getResourceAsInputStream(URI location) throws FileNotFoundException {
        if ("classpath".equalsIgnoreCase(location.getScheme())) {
             return SSLContextSupplier.class.getClassLoader().getResourceAsStream(location.getSchemeSpecificPart());
        }
        return new FileInputStream(isNullOrEmpty(location.getPath()) ? location.getSchemeSpecificPart() : location.getPath());
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore)
            throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] pass = keyStoreProperties.getPassword().toCharArray();
        keyManagerFactory.init(keyStore, pass);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }
}
