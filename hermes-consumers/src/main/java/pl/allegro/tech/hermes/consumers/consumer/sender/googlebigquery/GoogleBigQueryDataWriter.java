package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class GoogleBigQueryDataWriter<
        T,
        S extends AutoCloseable,
        F extends GoogleBigQueryStreamWriterFactory<S>> implements SenderClient<T> {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryDataWriter.class);

    protected final S streamWriter;

    public GoogleBigQueryDataWriter(String streamName, F factory) {
        streamWriter = factory.getWriterForStream(streamName);
    }

    protected abstract ApiFuture<AppendRowsResponse> append(T message) throws Descriptors.DescriptorValidationException, IOException;
    protected abstract String getWriterId();
    protected abstract String getStreamName();

    @Override
    public void publish(T message, CompletableFuture<MessageSendingResult> resultFuture) throws IOException, ExecutionException, InterruptedException {
        try {
            ApiFuture<AppendRowsResponse> appendFuture = append(message);
            ApiFutures.addCallback(appendFuture, new GoogleBigQueryAppendCompleteCallback(resultFuture), MoreExecutors.directExecutor());
        } catch (Exceptions.AppendSerializationError e) {
            logger.warn("Writer {} has failed to append rows to stream {}", getWriterId(), getStreamName(), e);
            for (Map.Entry<Integer, String> entry : e.getRowIndexToErrorMessage().entrySet()) {
                logger.warn("Writer {} has failed because of {}", getWriterId(), entry.getValue(), e);
            }

            resultFuture.complete(MessageSendingResult.failedResult(new GoogleBigQueryFailedAppendException(e)));
        } catch (Exception e) {
            logger.warn("Writer {} has failed to append rows to stream {}", getWriterId(), getStreamName(), e);
            resultFuture.complete(MessageSendingResult.failedResult(e));
        }
    }

    @Override
    public void shutdown() {
        try {
            streamWriter.close();
        } catch (Exception e) {
            logger.error("Error during closing stream writer of id {} and name {}", getWriterId(), getStreamName());
        }
    }
}
