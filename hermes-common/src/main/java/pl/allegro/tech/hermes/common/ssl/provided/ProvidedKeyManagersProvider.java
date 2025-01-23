package pl.allegro.tech.hermes.common.ssl.provided;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import pl.allegro.tech.hermes.common.ssl.KeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;

public class ProvidedKeyManagersProvider implements KeyManagersProvider, ResourceLoader {

  private final KeystoreProperties keystoreProperties;

  public ProvidedKeyManagersProvider(KeystoreProperties keystoreProperties) {
    this.keystoreProperties = keystoreProperties;
  }

  @Override
  public KeyManager[] getKeyManagers() throws Exception {
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    KeyStore keyStore = KeyStore.getInstance(keystoreProperties.getFormat());
    try (InputStream stream = getResourceAsInputStream(keystoreProperties.getLocationAsURI())) {
      keyStore.load(stream, keystoreProperties.getPassword().toCharArray());
      keyManagerFactory.init(keyStore, keystoreProperties.getPassword().toCharArray());
    }
    return keyManagerFactory.getKeyManagers();
  }
}
