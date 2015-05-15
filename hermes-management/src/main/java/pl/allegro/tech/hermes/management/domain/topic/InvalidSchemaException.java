package pl.allegro.tech.hermes.management.domain.topic;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import java.util.List;

public class InvalidSchemaException extends ManagementException {

    public InvalidSchemaException(Throwable cause) {
        super("Error while trying to validate schema", cause);
    }

    public InvalidSchemaException(List<String> reasons) {
        super(String.format("Tried to set invalid topic schema: %s", Joiner.on(";").join(reasons)));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.FORMAT_ERROR;
    }
}
