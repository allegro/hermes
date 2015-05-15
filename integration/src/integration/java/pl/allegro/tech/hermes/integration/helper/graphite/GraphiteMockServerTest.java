package pl.allegro.tech.hermes.integration.helper.graphite;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class GraphiteMockServerTest {

    public static final int PORT = 13536;

    private GraphiteMockServer graphiteMockServer;
    private Socket client;

    @BeforeMethod
    public void setUp() throws IOException {
        graphiteMockServer = new GraphiteMockServer(PORT);
        graphiteMockServer.start();

        client = new Socket("localhost", PORT);
    }

    @AfterMethod
    public void clean() throws IOException {
        graphiteMockServer.stop();
    }

    @Test
    public void shouldExpectMetricWithWildcard() throws IOException {
        //given
        graphiteMockServer.expectMetric("metric.*.name", 1);
        DataOutputStream writer = new DataOutputStream(client.getOutputStream());

        //when
        writer.writeBytes("metric.host.name 1 213432");
        writer.flush();
        writer.close();

        //then
        graphiteMockServer.waitUntilReceived();
    }

}