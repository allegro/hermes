package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface HermesSender {

    String SCHEMA_VERSION_HEADER = "Schema-Version";

    CompletableFuture<HermesResponse> send(URI uri, HermesMessage message);
}
