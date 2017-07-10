package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.MatcherInputException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class MatcherExceptionMapper extends AbstractExceptionMapper<MatcherInputException> {

    @Override
    Response.Status httpStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    ErrorCode errorCode() {
        return ErrorCode.INVALID_QUERY;
    }
}
