package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class HermesMock {
    private final WireMockServer wireMockServer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private int awaitSeconds = 5;

    private final HermesMockDefine hermesMockDefine;
    private final HermesMockExpect hermesMockExpect;
    private final HermesMockQuery hermesMockQuery;

    public HermesMock() {
        this(wireMockConfig().portNumber());
    }

    public HermesMock(int port) {
        if (port == 0) {
            port = wireMockConfig().dynamicPort().portNumber();
        }
        wireMockServer = new WireMockServer(port);

        HermesMockHelper hermesMockHelper = new HermesMockHelper(wireMockServer, objectMapper);
        hermesMockDefine = new HermesMockDefine(hermesMockHelper);
        hermesMockExpect = new HermesMockExpect(hermesMockHelper, awaitSeconds);
        hermesMockQuery = new HermesMockQuery(hermesMockHelper);
    }

    public HermesMockDefine define() {
        return hermesMockDefine;
    }

    public HermesMockExpect expect() {
        return hermesMockExpect;
    }

    public HermesMockQuery query() {
        return hermesMockQuery;
    }

    public void resetReceivedRequest() {
        wireMockServer.resetRequests();
    }

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.stop();
    }

    public static class Builder {
        private int port;
        private int awaitSeconds;
        private ObjectMapper objectMapper;

        public Builder() {
            awaitSeconds = 5;
            objectMapper = new ObjectMapper();
        }

        public HermesMock.Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public HermesMock.Builder withObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public HermesMock.Builder withAwaitSeconds(int awaitSeconds) {
            this.awaitSeconds = awaitSeconds;
            return this;
        }

        public HermesMock build() {
            HermesMock hermesMock = new HermesMock(port);
            hermesMock.awaitSeconds = awaitSeconds;
            hermesMock.objectMapper = objectMapper;
            return hermesMock;
        }
    }
}
