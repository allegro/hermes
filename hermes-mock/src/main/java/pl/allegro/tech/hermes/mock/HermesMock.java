package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

public class HermesMock {
    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private int awaitSeconds = 5;

    private HermesMockHelper hermesMockHelper;
    private HermesMockDefine hermesMockDefine;
    private HermesMockExpect hermesMockExpect;
    private HermesMockQuery hermesMockQuery;

    public HermesMock() {
        wireMockServer = new WireMockServer();
        initComponents();
    }

    public HermesMock(int port) {
        wireMockServer = new WireMockServer(port);
        initComponents();
    }

    private void initComponents() {
        hermesMockHelper = new HermesMockHelper(wireMockServer, objectMapper);
        hermesMockDefine = new HermesMockDefine(wireMockServer);
        hermesMockExpect = new HermesMockExpect(wireMockServer, awaitSeconds, hermesMockHelper);
        hermesMockQuery = new HermesMockQuery(wireMockServer, hermesMockHelper);
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
            HermesMock hermesMock;
            if (port == 0) {
                hermesMock = new HermesMock();
            } else {
                hermesMock = new HermesMock(port);
            }
            hermesMock.awaitSeconds = awaitSeconds;
            hermesMock.objectMapper = objectMapper;
            return hermesMock;
        }
    }
}
