package pl.allegro.tech.hermes.frontend.utils;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFuturesHelper {

  public static <T> CompletableFuture<List<T>> allComplete(List<CompletableFuture<T>> futures) {
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(toList()));
  }
}
