package pl.allegro.tech.hermes.integration.test;

import javax.ws.rs.core.Response;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;

public class HttpResponseAssertion extends AbstractAssert<HttpResponseAssertion, Response> {

    HttpResponseAssertion(Response actual) {
        super(actual, HttpResponseAssertion.class);
    }

    public HttpResponseAssertion hasStatus(Response.Status status) {
        assertThat(actual.getStatus()).isEqualTo(status.getStatusCode());
        return this;
    }

    public HttpResponseAssertion hasErrorCode(ErrorCode errorCode) {
        ErrorDescription error = actual.readEntity(ErrorDescription.class);
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(errorCode);
        return this;
    }
}
