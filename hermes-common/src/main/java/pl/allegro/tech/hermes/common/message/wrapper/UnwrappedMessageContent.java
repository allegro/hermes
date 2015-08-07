package pl.allegro.tech.hermes.common.message.wrapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class UnwrappedMessageContent {

    private final MessageMetadata messageMetadata;
    private final byte[] content;

    public UnwrappedMessageContent(MessageMetadata messageMetadata, byte[] content) {
        this.messageMetadata = messageMetadata;
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public MessageMetadata getMessageMetadata() {
        return messageMetadata;
    }
}
