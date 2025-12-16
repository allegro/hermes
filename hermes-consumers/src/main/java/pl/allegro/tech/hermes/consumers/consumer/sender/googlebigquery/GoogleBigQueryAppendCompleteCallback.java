package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class GoogleBigQueryAppendCompleteCallback implements ApiFutureCallback<AppendRowsResponse> {

  private final CompletableFuture<MessageSendingResult> resultFuture;

  public GoogleBigQueryAppendCompleteCallback(
      CompletableFuture<MessageSendingResult> resultFuture) {
    this.resultFuture = resultFuture;
  }

  @Override
  public void onFailure(Throwable t) {
    Exceptions.StorageException storageException = Exceptions.toStorageException(t);
    resultFuture.complete(
        MessageSendingResult.failedResult(Objects.requireNonNullElse(storageException, t)));
  }

  @Override
  public void onSuccess(AppendRowsResponse result) {
    resultFuture.complete(MessageSendingResult.succeededResult());
  }
}
