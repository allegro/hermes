package pl.allegro.tech.hermes.client;

import java.net.URI;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface ReactiveHermesSender {

  Mono<HermesResponse> sendReactively(URI uri, HermesMessage message);
}
