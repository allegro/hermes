package pl.allegro.tech.hermes.integration.test;

import org.assertj.core.api.AbstractAssert;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

import java.util.Arrays;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseAssertion extends AbstractAssert<HttpResponseAssertion, Response> {

    HttpResponseAssertion(Response actual) {
        super(actual, HttpResponseAssertion.class);
    }

    public HttpResponseAssertion hasStatus(Response.Status status) {
        assertThat(actual.getStatus()).isEqualTo(status.getStatusCode());
        return this;
    }

    public HttpResponseAssertion hasStatusFamily(Response.Status.Family statusFamily) {
        assertThat(actual.getStatusInfo().getFamily()).isEqualTo(statusFamily);
        return this;
    }

    public HttpResponseAssertion hasErrorCode(ErrorCode errorCode) {
        ErrorDescription error = actual.readEntity(ErrorDescription.class);
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(errorCode);
        return this;
    }

    public HttpResponseAssertion containsMessage(String msg) {
        assertThat(actual.readEntity(String.class)).contains(msg);
        return this;
    }

    public HttpResponseAssertion containsMessages(String... messages) {
        String responseBody = actual.readEntity(String.class);
        assertThat(Arrays.stream(messages).allMatch(responseBody::contains)).isTrue();
        return this;
    }
}
