package pl.allegro.tech.hermes.common.message.wrapper;

public interface MessageContentWrapper {
    MessageWithMetadata unwrapContent(byte[] data);
    byte[] wrapContent(byte[] data, String id, long timestamp);
}
