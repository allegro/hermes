package pl.allegro.tech.hermes.common.ssl.jvm;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import pl.allegro.tech.hermes.common.ssl.TrustManagersProvider;

public class JvmTrustManagerProvider implements TrustManagersProvider {

  @Override
  public TrustManager[] getTrustManagers() throws Exception {
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(loadJvmKeyStore());
    return trustManagerFactory.getTrustManagers();
  }

  private KeyStore loadJvmKeyStore() throws Exception {
    String trustStore = System.getProperty("javax.net.ssl.trustStore", "");
    String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
    String trustStoreType =
        System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
    String trustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider", "");

    KeyStore keyStore =
        trustStoreProvider.isEmpty()
            ? KeyStore.getInstance(trustStoreType)
            : KeyStore.getInstance(trustStoreType, trustStoreProvider);
    char[] password = trustStorePassword == null ? null : trustStorePassword.toCharArray();
    keyStore.load(trustStoreInputStream(trustStore), password);

    return keyStore;
  }

  private InputStream trustStoreInputStream(String trustStore) throws FileNotFoundException {
    return new FileInputStream(trustStore.isEmpty() ? defaultTrustStore() : new File(trustStore));
  }

  private File defaultTrustStore() throws FileNotFoundException {
    String javaHome = System.getProperty("java.home");

    File jssecacerts = new File(format("%s/lib/security/jssecacerts", javaHome));
    if (jssecacerts.exists()) {
      return jssecacerts;
    }

    File cacerts = new File(format("%s/lib/security/cacerts", javaHome));
    if (cacerts.exists()) {
      return cacerts;
    }

    throw new FileNotFoundException("Default trust store not found.");
  }
}
