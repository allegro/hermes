package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.Exceptions;

public class GoogleBigQueryFailedAppendException extends RuntimeException {

    public GoogleBigQueryFailedAppendException(Exceptions.AppendSerializtionError cause) {
        super(String.join("\n", cause.getRowIndexToErrorMessage().values()), cause);
    }
}
