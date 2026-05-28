package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import io.grpc.Status;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class GoogleBigQueryAppendCompleteCallback implements ApiFutureCallback<AppendRowsResponse> {

  private static final Logger logger =
      LoggerFactory.getLogger(GoogleBigQueryAppendCompleteCallback.class);

  private final CompletableFuture<MessageSendingResult> resultFuture;

  public GoogleBigQueryAppendCompleteCallback(
      CompletableFuture<MessageSendingResult> resultFuture) {
    this.resultFuture = resultFuture;
  }

  @Override
  public void onFailure(Throwable t) {
    Exceptions.StorageException storageException = Exceptions.toStorageException(t);
    Throwable cause = Objects.requireNonNullElse(storageException, t);

    Integer httpStatusCode = mapToPermanentErrorHttpStatus(cause);
    if (httpStatusCode != null) {
      logger.warn("BigQuery permanent error mapped to HTTP {}: {}", httpStatusCode, cause.getMessage(), cause);
      resultFuture.complete(MessageSendingResult.failedResult(httpStatusCode, cause));
    } else {
      resultFuture.complete(MessageSendingResult.failedResult(cause));
    }
  }

  @Override
  public void onSuccess(AppendRowsResponse result) {
    resultFuture.complete(MessageSendingResult.succeededResult());
  }

  private static Integer mapToPermanentErrorHttpStatus(Throwable cause) {
    Status.Code grpcCode = Status.fromThrowable(cause).getCode();
    return switch (grpcCode) {
      case NOT_FOUND -> 404; // Table does not exist
      case PERMISSION_DENIED -> 403; // Technical user does not have permissions to write to the table
      case INVALID_ARGUMENT -> 400; // Invalid message format i.e. microsecond timestamp value is sent to millisecond timestamp field
      default -> null;
    };
  }
}
