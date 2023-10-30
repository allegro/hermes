package pl.allegro.tech.hermes.integrationtests;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.function.Consumer;

public class TestSubscriber {

    public String getEndpoint() {
        return "http://localhost:8080/";
    }

    public void waitUntilReceived(String body) {

    }

    public void waitUntilRequestReceived(Consumer<LoggedRequest> requestConsumer) {

    }
}
