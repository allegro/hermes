package pl.allegro.tech.hermes.common.ssl;

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

import static com.google.common.base.Strings.isNullOrEmpty;

public class JvmKeystoreSslContextFactory implements SslContextFactory {
    private final String protocol;
    private final KeystoreProperties keyStoreProperties;
    private final KeystoreProperties trustStoreProperties;

    public JvmKeystoreSslContextFactory(String protocol, KeystoreProperties keyStoreProperties, KeystoreProperties trustStoreProperties) {
        this.protocol = protocol;
        this.keyStoreProperties = keyStoreProperties;
        this.trustStoreProperties = trustStoreProperties;
    }

    @Override
    public SSLContext create() {
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
             return JvmKeystoreSslContextFactory.class.getClassLoader().getResourceAsStream(location.getSchemeSpecificPart());
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
