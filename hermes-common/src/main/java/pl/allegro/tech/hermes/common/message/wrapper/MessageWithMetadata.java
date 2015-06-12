package pl.allegro.tech.hermes.common.message.wrapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class MessageWithMetadata {

    private final MessageMetadata messageMetadata;
    private final byte[] content;

    public MessageWithMetadata(MessageMetadata messageMetadata, byte[] content) {
        this.messageMetadata = messageMetadata;
        this.content = content;
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    public MessageMetadata getMessageMetadata() {
        return messageMetadata;
    }
}
