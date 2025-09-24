package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.bigquery.storage.v1.Exceptions;
import java.util.Map;
import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class GoogleBigQueryFailedAppendExceptionTest {
  @Test
  public void testGoogleBigQueryFailedAppendException() {
    // given
    int codeValue = 1;
    String description = "NullPointerException";
    String streamName = "streamName";

    Map<Integer, String> rowIndexToErrorMessage = null;

    Exceptions.AppendSerializtionError cause =
        new Exceptions.AppendSerializationError(
            codeValue, description, streamName, rowIndexToErrorMessage);
    GoogleBigQueryFailedAppendException exception = new GoogleBigQueryFailedAppendException(cause);
    // when
    String message = exception.getMessage();

    // then
    assertThat(message).contains("CANCELLED: NullPointerException");
    assertThat(message)
        .contains("GoogleBigQuery Subscription has failed to append rows to stream streamName");
  }

  @Test
  public void testGoogleBigQueryFailedAppendExceptionWithColumns() {
    // given
    int codeValue = 1;
    String description = "NullPointerException";
    String streamName = "streamName";

    Map<Integer, String> rowIndexToErrorMessage =
        Map.of(
            1, "Column1 not found",
            2, "Column2 has different type");

    Exceptions.AppendSerializtionError cause =
        new Exceptions.AppendSerializationError(
            codeValue, description, streamName, rowIndexToErrorMessage);
    GoogleBigQueryFailedAppendException exception = new GoogleBigQueryFailedAppendException(cause);
    // when
    String message = exception.getMessage();
    System.out.println(message);

    // then
    assertThat(message)
        .contains("GoogleBigQuery Subscription has failed to append rows to stream streamName");
    assertThat(message).contains("CANCELLED: NullPointerException");
    assertThat(message)
        .contains("GoogleBigQuery Subscription has failed because of Column1 not found");
    assertThat(message)
        .contains("GoogleBigQuery Subscription has failed because of Column2 has different type");
  }

  @Test
  public void testSendingResultNullPointer() {
    // given
    MessageSendingResult singleMessageSendingResult = null;
    try {
      String a = null;
      boolean result = a.contains("other");
    } catch (Exception e) {
      singleMessageSendingResult = MessageSendingResult.failedResult(new RuntimeException(e));
    }

    // when
    String rootCause = singleMessageSendingResult.getRootCause();

    // then
    assertThat(rootCause)
        .isEqualTo(
            "Cannot invoke \"String.contains(java.lang.CharSequence)\" because \"a\" is null");
  }

  @Test
  public void testSendingResultFailedAppend() {
    // given
    MessageSendingResult singleMessageSendingResult = null;
    try {
      String a = null;
      boolean result = a.contains("other");
    } catch (Exception e) {
      singleMessageSendingResult =
          MessageSendingResult.failedResult(
              new GoogleBigQueryFailedAppendException(
                  new Exceptions.AppendSerializationError(
                      1, "NullPointerException", "streamName", Map.of(1, "Column1 not found"))));
    }

    // when
    String rootCause = singleMessageSendingResult.getRootCause();

    // then
    assertThat(rootCause).isEqualTo("CANCELLED: NullPointerException");
  }
}
