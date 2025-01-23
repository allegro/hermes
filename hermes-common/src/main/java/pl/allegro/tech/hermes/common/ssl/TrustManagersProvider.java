package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.TrustManager;

public interface TrustManagersProvider {
  TrustManager[] getTrustManagers() throws Exception;
}
