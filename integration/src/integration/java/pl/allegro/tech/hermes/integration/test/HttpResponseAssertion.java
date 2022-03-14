package pl.allegro.tech.hermes.integration.test;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

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

    public HttpResponseAssertion hasStatus(List<Response.Status> statuses) {
        assertThat(statuses.contains(Response.Status.fromStatusCode(actual.getStatus()))).isEqualTo(true);
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

    public HttpResponseAssertion containsMessages(String ...messages) {
        String responseBody = actual.readEntity(String.class);
        assertThat(Arrays.stream(messages).allMatch(responseBody::contains)).isTrue();
        return this;
    }
}
