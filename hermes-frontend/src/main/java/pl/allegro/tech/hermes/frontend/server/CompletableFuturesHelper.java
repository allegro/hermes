package pl.allegro.tech.hermes.frontend.server;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

class CompletableFuturesHelper {

    static <T> CompletableFuture<List<T>> allComplete(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(toList()));
    }
}
