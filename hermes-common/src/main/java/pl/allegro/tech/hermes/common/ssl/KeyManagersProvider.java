package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.KeyManager;

public interface KeyManagersProvider {
  KeyManager[] getKeyManagers() throws Exception;
}
