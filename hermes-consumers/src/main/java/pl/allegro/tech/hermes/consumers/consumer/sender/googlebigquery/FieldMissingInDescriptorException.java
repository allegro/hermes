package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

public class FieldMissingInDescriptorException extends Exception {
    public FieldMissingInDescriptorException(String message, Exception e) {
        super(message, e);
    }
}
