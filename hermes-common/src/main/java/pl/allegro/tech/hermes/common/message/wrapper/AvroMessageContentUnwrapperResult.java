package pl.allegro.tech.hermes.common.message.wrapper;

class AvroMessageContentUnwrapperResult {

    private final UnwrappedMessageContent content;
    private final AvroMessageContentUnwrapperResultStatus status;

    private AvroMessageContentUnwrapperResult(UnwrappedMessageContent content,
                                              AvroMessageContentUnwrapperResultStatus status) {
        this.content = content;
        this.status = status;
    }

    static public AvroMessageContentUnwrapperResult success(UnwrappedMessageContent content) {
        return new AvroMessageContentUnwrapperResult(content, AvroMessageContentUnwrapperResultStatus.SUCCESS);
    }

    static public AvroMessageContentUnwrapperResult failure() {
        return new AvroMessageContentUnwrapperResult(null, AvroMessageContentUnwrapperResultStatus.FAILURE);
    }

    public UnwrappedMessageContent getContent() {
        return content;
    }

    public AvroMessageContentUnwrapperResultStatus getStatus() {
        return status;
    }

    enum AvroMessageContentUnwrapperResultStatus {
        SUCCESS, FAILURE
    }
}
