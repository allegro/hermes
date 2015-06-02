package pl.allegro.tech.hermes.client.jersey;

import pl.allegro.tech.hermes.client.HermesResponse;

import javax.ws.rs.core.Response;

class JerseyHermesResponse implements HermesResponse {
    private final Response response;
    private final String body;

    public JerseyHermesResponse(Response response) {
        this.response = response;
        this.body = response.readEntity(String.class);
    }

    @Override
    public int getHttpStatus() {
        return response.getStatus();
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getHeader(String header) {
        return response.getHeaderString(header);
    }
}
