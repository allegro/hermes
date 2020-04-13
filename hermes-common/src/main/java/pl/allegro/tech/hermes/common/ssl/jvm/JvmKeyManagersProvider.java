package pl.allegro.tech.hermes.common.ssl.jvm;

import pl.allegro.tech.hermes.common.ssl.KeyManagersProvider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class JvmKeyManagersProvider implements KeyManagersProvider {

    @Override
    public KeyManager[] getKeyManagers() throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(null, null);
        return keyManagerFactory.getKeyManagers();
    }
}
