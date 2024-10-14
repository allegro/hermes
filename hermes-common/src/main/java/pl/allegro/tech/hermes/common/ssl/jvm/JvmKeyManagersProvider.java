package pl.allegro.tech.hermes.common.ssl.jvm;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import pl.allegro.tech.hermes.common.ssl.KeyManagersProvider;

public class JvmKeyManagersProvider implements KeyManagersProvider {

  @Override
  public KeyManager[] getKeyManagers() throws Exception {
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(null, null);
    return keyManagerFactory.getKeyManagers();
  }
}
