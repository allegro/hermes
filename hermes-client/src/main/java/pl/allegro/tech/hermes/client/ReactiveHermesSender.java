package pl.allegro.tech.hermes.client;

import reactor.core.publisher.Mono;

import java.net.URI;

@FunctionalInterface
public interface ReactiveHermesSender {

    Mono<HermesResponse> sendReactively(URI uri, HermesMessage message);
}
