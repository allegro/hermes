package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData

class TestHttpRequestData {

    static HttpRequestData requestData() {
        return new HttpRequestData.HttpRequestDataBuilder().
                withRawAddress(EndpointAddress.of("http://localhost:8080"))
                .build();
    }
}
