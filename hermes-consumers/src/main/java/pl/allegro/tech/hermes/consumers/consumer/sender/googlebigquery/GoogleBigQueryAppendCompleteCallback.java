package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import io.grpc.Status;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class GoogleBigQueryAppendCompleteCallback implements ApiFutureCallback<AppendRowsResponse> {

  private final CompletableFuture<MessageSendingResult> resultFuture;

  public GoogleBigQueryAppendCompleteCallback(
      CompletableFuture<MessageSendingResult> resultFuture) {
    this.resultFuture = resultFuture;
  }

  public static Integer mapToPermanentErrorHttpStatus(Throwable cause) {
    Status.Code grpcCode = Status.fromThrowable(cause).getCode();
    return switch (grpcCode) {
      case NOT_FOUND -> 404; // Table does not exist
      case PERMISSION_DENIED ->
          403; // Technical user does not have permissions to write to the table
      case INVALID_ARGUMENT ->
          400; // Invalid message format i.e. microsecond timestamp value is sent to millisecond
      // timestamp field
      default -> 500;
    };
  }

  @Override
  public void onFailure(Throwable t) {
    Exceptions.StorageException storageException = Exceptions.toStorageException(t);
    Throwable cause = Objects.requireNonNullElse(storageException, t);

    Integer httpStatusCode = mapToPermanentErrorHttpStatus(cause);
    resultFuture.complete(MessageSendingResult.failedResult(httpStatusCode, cause));
  }

  @Override
  public void onSuccess(AppendRowsResponse result) {
    resultFuture.complete(MessageSendingResult.succeededResult());
  }
}
