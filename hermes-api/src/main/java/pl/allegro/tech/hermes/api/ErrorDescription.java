package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorDescription {

    private final String message;
    private final ErrorCode code;

    @JsonCreator
    public ErrorDescription(@JsonProperty("message") String message, @JsonProperty("code") ErrorCode code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorCode getCode() {
        return code;
    }
}
