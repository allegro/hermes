package pl.allegro.tech.hermes.client.okhttp;

import com.squareup.okhttp.*;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

public class OkHttpSender implements HermesSender {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;

    public OkHttpSender() {
        client = new OkHttpClient();
        SSLContext sslContext = prepareSSLContext();
        client.setSslSocketFactory(sslContext.getSocketFactory());
        client.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();

        RequestBody body = RequestBody.create(JSON, message.getBody());
        Request request = new Request.Builder()
                .post(body)
                .url(uri.toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                future.complete(hermesResponse().withHttpStatus(response.code()).build());
            }
        });

        return future;
    }

    private static final String format = "JKS";
    private static final String password = "password";
    private static final String keystore = "client.keystore";
    private static final String truststore = "client.truststore";
    private static final String protocol = "TLS";

    private SSLContext prepareSSLContext() {
        try {
            return createSSLContext(loadKeyStore(keystore, password, format),
                    loadKeyStore(truststore, password, format));
        } catch (Exception e) {
            throw new IllegalStateException("Something went wrong with setting up SSL context.", e);
        }
    }

    private KeyStore loadKeyStore(String name, String password, String format) throws Exception {
        try (InputStream stream = OkHttpSender.class.getClassLoader().getResourceAsStream(name)) {
            KeyStore loadedKeystore = KeyStore.getInstance(format);
            loadedKeystore.load(stream, password.toCharArray());
            return loadedKeystore;
        }
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] pass = password.toCharArray();
        keyManagerFactory.init(keyStore, pass);
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance(protocol);
        sslContext.init(keyManagers, trustManagers, SecureRandom.getInstanceStrong());

        return sslContext;
    }
}
