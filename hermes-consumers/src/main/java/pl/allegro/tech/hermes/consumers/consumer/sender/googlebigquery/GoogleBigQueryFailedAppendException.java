package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.Exceptions;

import java.util.Map;

public class GoogleBigQueryFailedAppendException extends RuntimeException {
    @Override
    public String getMessage() {
        Exceptions.AppendSerializtionError cause = ((Exceptions.AppendSerializtionError)this.getCause());
        StringBuilder message = new StringBuilder(String.format("GoogleBigQuery Subscription has failed to append rows to stream %s", cause.getStreamName()));
        message.append(String.format("\n%s", super.getMessage()));
        if (cause.getRowIndexToErrorMessage() != null) {
            for (Map.Entry<Integer, String> entry : cause.getRowIndexToErrorMessage().entrySet()) {
                message.append(String.format("\nGoogleBigQuery Subscription has failed because of %s", entry.getValue()));
            }
        }
        return message.toString();
    }

    public GoogleBigQueryFailedAppendException(Exceptions.AppendSerializtionError cause) {
        super(cause.getMessage(), cause);
    }
}
