package pl.allegro.tech.hermes.client.restTemplate;

import org.springframework.http.ResponseEntity;
import pl.allegro.tech.hermes.client.HermesResponse;

class RestTemplateHermesResponse implements HermesResponse {
    private final ResponseEntity result;

    public RestTemplateHermesResponse(ResponseEntity result) {
        this.result = result;
    }

    @Override
    public int getHttpStatus() {
        return result.getStatusCode().value();
    }

    @Override
    public String getBody() {
        return result.toString();
    }

    @Override
    public String getHeader(String header) {
        return result.getHeaders().toSingleValueMap().getOrDefault(header, "");
    }

}
