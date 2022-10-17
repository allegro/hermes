package pl.allegro.tech.hermes.common.ssl.provided;

import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.TrustManagersProvider;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class ProvidedTrustManagersProvider implements TrustManagersProvider, ResourceLoader {

    private final KeystoreProperties keystoreProperties;

    public ProvidedTrustManagersProvider(KeystoreProperties keystoreProperties) {
        this.keystoreProperties = keystoreProperties;
    }

    @Override
    public TrustManager[] getTrustManagers() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(keystoreProperties.getFormat());
        try (InputStream stream = getResourceAsInputStream(keystoreProperties.getLocationAsURI())) {
            keyStore.load(stream, keystoreProperties.getPassword().toCharArray());
            trustManagerFactory.init(keyStore);
        }
        return trustManagerFactory.getTrustManagers();
    }
}
