package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface HermesSender {

  CompletableFuture<HermesResponse> send(URI uri, HermesMessage message);
}
