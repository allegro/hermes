package pl.allegro.tech.hermes.consumers.consumer.sender;

public interface ProtocolMessageSenderProvider {
    MessageSender create(String endpoint);

    void start() throws Exception;

    void stop() throws Exception;
}
