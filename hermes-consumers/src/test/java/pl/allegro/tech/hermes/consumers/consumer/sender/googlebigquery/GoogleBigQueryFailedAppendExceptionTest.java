package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.Exceptions;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

public class GoogleBigQueryFailedAppendExceptionTest {
    @Test
    public void testGoogleBigQueryFailedAppendException() {
        // Given
        int codeValue = 1;
        String description = "NullPointerException";
        String streamName = "streamName";

        Map<Integer, String> rowIndexToErrorMessage = null;

        Exceptions.AppendSerializtionError cause = new Exceptions.AppendSerializationError(
            codeValue,
            description,
            streamName,
            rowIndexToErrorMessage
        );
        GoogleBigQueryFailedAppendException exception = new GoogleBigQueryFailedAppendException(cause);
        // When
        String message = exception.getMessage();
        System.out.println(message);

        // Then
        assertThat(message).contains("CANCELLED: NullPointerException");
        assertThat(message).contains("GoogleBigQuery Subscription has failed to append rows to stream streamName");

    }

    @Test
    public void testGoogleBigQueryFailedAppendExceptionWithColumns() {
        // Given
        int codeValue = 1;
        String description = "NullPointerException";
        String streamName = "streamName";

        Map<Integer, String> rowIndexToErrorMessage = Map.of(
                1, "Column1 not found",
                2, "Column2 has different type"
        );

        Exceptions.AppendSerializtionError cause = new Exceptions.AppendSerializationError(
                codeValue,
                description,
                streamName,
                rowIndexToErrorMessage
        );
        GoogleBigQueryFailedAppendException exception = new GoogleBigQueryFailedAppendException(cause);
        // When
        String message = exception.getMessage();
        System.out.println(message);

        // Then
        assertThat(message).contains("GoogleBigQuery Subscription has failed to append rows to stream streamName");
        assertThat(message).contains("CANCELLED: NullPointerException");
        assertThat(message).contains("GoogleBigQuery Subscription has failed because of Column1 not found");
        assertThat(message).contains("GoogleBigQuery Subscription has failed because of Column2 has different type");

    }
}
