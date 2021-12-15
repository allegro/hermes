package pl.allegro.tech.hermes.tracker.consumers;

import java.util.HashMap;
import java.util.Map;

public class TestMessageMetadata {

    public static MessageMetadata of(String messageId, String topic, String subscription) {
        return of(messageId, topic, subscription, 1L, 1);
    }

    public static MessageMetadata of(String messageId, String topic, String subscription, Map<String, String> extraRequestHeaders) {
        return of(messageId, topic, subscription, 1L, 1, extraRequestHeaders);
    }

    public static MessageMetadata of(String messageId, String topic, String subscription, long offset, int partition) {
        return of(messageId, topic, subscription, offset, partition, sampleExtraHeaders());
    }

    public static MessageMetadata of(String messageId, String topic, String subscription, long offset, int partition, Map<String, String> extraRequestHeaders) {
        return new MessageMetadata(messageId, offset, partition, 123L, topic, subscription, topic, 123456L, 123456L, extraRequestHeaders);
    }

    public static MessageMetadata of(String messageId,String batchId, String topic, String subscription) {
        return of(messageId, batchId, topic, subscription, 1L, 1);
    }

    public static MessageMetadata of(String messageId,String batchId, String topic, String subscription, Map<String, String> extraRequestHeaders) {
        return of(messageId, batchId, topic, subscription, 1L, 1, extraRequestHeaders);
    }

    public static MessageMetadata of(String messageId, String batchId, String topic, String subscription, long offset, int partition) {
        return of(messageId, batchId, topic, subscription, offset, partition, sampleExtraHeaders());
    }

    public static MessageMetadata of(String messageId, String batchId, String topic, String subscription, long offset, int partition, Map<String, String> extraRequestHeaders) {
        return new MessageMetadata(messageId, batchId, offset, partition, 123L, topic, subscription, topic, 123456L, 123456L, extraRequestHeaders);
    }

    private static Map<String, String> sampleExtraHeaders() {
        Map<String, String> sampleHeaders = new HashMap<>();
        sampleHeaders.put("x-header1", "value1");
        sampleHeaders.put("x-header2", "value-2");
        return sampleHeaders;
    }
}
