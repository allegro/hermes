package pl.allegro.tech.hermes.common.exception;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.ErrorCode;

import java.util.List;

public class InvalidSchemaException extends HermesException {

    public InvalidSchemaException(Throwable cause) {
        super("Error while trying to validate schema", cause);
    }

    public InvalidSchemaException(List<String> reasons) {
        super(String.format("Tried to set invalid topic schema: %s", Joiner.on(";").join(reasons)));
    }

    public InvalidSchemaException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.FORMAT_ERROR;
    }
}
