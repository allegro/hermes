package pl.allegro.tech.hermes.test.helper.oauth.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class OAuthTestServer {

    public static final String OAUTH2_TOKEN_ENDPOINT = "/oauth2/token";
    public static final String OAUTH2_RESOURCE_ENDPOINT = "/oauth2/resource";

    private final OAuthTestServerStorage storage = new OAuthTestServerStorage();
    private final Undertow undertow;
    private final int port;

    public OAuthTestServer() {
        port = Ports.nextAvailable();

        this.undertow = Undertow.builder()
            .addHttpListener(port, "localhost")
            .setHandler(createPathHandler())
            .build();
    }

    private PathHandler createPathHandler() {
        try {
            Servlet tokenServlet = new OAuthAccessTokenServlet(storage);
            Servlet resourceServlet = new OAuthResourceServlet(storage);

            DeploymentInfo deploymentInfo = Servlets.deployment()
                    .setClassLoader(OAuthTestServer.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName("OAuthTestServer")
                    .addServlets(
                            Servlets.servlet("AuthTokenServlet", OAuthAccessTokenServlet.class,
                                    new ImmediateInstanceFactory<>(tokenServlet))
                                    .addMapping(OAUTH2_TOKEN_ENDPOINT),
                            Servlets.servlet("ResourceServlet", OAuthResourceServlet.class,
                                    new ImmediateInstanceFactory<>(resourceServlet))
                                    .addMapping(OAUTH2_RESOURCE_ENDPOINT)
                    );

            DeploymentManager deploymentManager = Servlets.defaultContainer().addDeployment(deploymentInfo);
            deploymentManager.deploy();
            HttpHandler servletHandler = deploymentManager.start();
            return Handlers.path().addPrefixPath("/", servletHandler);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTokenEndpoint() {
        return String.format("http://localhost:%s%s", port, OAUTH2_TOKEN_ENDPOINT);
    }

    public String getUsernamePasswordSecuredResourceEndpoint(String username) {
        return String.format("http://localhost:%s%s?username=%s", port, OAUTH2_RESOURCE_ENDPOINT, username);
    }

    public String getClientCredentialsSecuredResourceEndpoint(String clientId) {
        return String.format("http://localhost:%s%s?clientId=%s", port, OAUTH2_RESOURCE_ENDPOINT, clientId);
    }

    public void registerClient(String clientId, String clientSecret) {
        storage.addClient(clientId, clientSecret);
    }

    public void registerResourceOwner(String username, String password) {
        storage.addResourceOwner(username, password);
    }

    public String issueAccessToken(String username) {
        return storage.issueToken(username);
    }

    public void clearStorage() {
        storage.clearAll();
    }

    public void unregisterAllClients() {
        storage.clearClients();
    }

    public void unregisterAllResourceOwners() {
        storage.clearOwners();
    }

    public void revokeAllTokens() {
        storage.clearTokens();
    }

    public void clearResourceAccessCounters() {
        storage.clearAccessCounters();
    }

    public int getResourceAccessCount(String owner) {
        return storage.getResourceAccessCount(owner);
    }

    public void clearTokenIssueCounters() {
        storage.clearTokenIssueCounters();
    }

    public int getTokenIssueCount(String username) {
        return storage.getTokenIssueCount(username);
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
