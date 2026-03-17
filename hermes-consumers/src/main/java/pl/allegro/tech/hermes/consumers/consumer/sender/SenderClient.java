package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface SenderClient<T> {

  void publish(T message, CompletableFuture<MessageSendingResult> resultFuture)
      throws IOException,
          ExecutionException,
          InterruptedException,
          Descriptors.DescriptorValidationException;

  void shutdown();
}
