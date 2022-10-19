package pl.allegro.tech.hermes.test.helper.oauth.server;

import io.undertow.util.Headers;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.TokenType;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static pl.allegro.tech.hermes.test.helper.oauth.server.OAuthServletResponseUtils.getOAuthJsonErrorResponse;
import static pl.allegro.tech.hermes.test.helper.oauth.server.OAuthServletResponseUtils.sendResponse;

class OAuthAccessTokenServlet extends HttpServlet {

    private final OAuthTestServerStorage storage;

    OAuthAccessTokenServlet(OAuthTestServerStorage storage) {
        this.storage = storage;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            OAuthTokenRequest request = new OAuthTokenRequest(req);
            validateClientCredentials(request);
            String token;
            if ("password".equals(request.getGrantType())) {
                validateResourceOwnerCredentials(request);
                token = storage.issueToken(request.getUsername());
            } else {
                token = storage.issueToken(request.getClientId());
            }
            OAuthResponse response = OAuthASResponse.tokenResponse(200)
                    .setAccessToken(token)
                    .setTokenType(TokenType.BEARER.toString())
                    .buildJSONMessage();
            sendResponse(resp, response.getBody(), response.getResponseStatus());

        } catch (OAuthProblemException e) {
            OAuthResponse response = getOAuthJsonErrorResponse(e, HttpServletResponse.SC_BAD_REQUEST);
            resp.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");
            sendResponse(resp, response.getBody(), response.getResponseStatus());
        } catch (OAuthSystemException e) {
            sendResponse(resp, e.getMessage(), 500);
        }
    }

    private void validateClientCredentials(OAuthTokenRequest request) throws OAuthProblemException {
        if (!storage.clientExists(request.getClientId(), request.getClientSecret())) {
            String message = String.format("No client with name %s registered or invalid client secret provided",
                    request.getClientId());
            throw OAuthProblemException.error(message);
        }
    }

    private void validateResourceOwnerCredentials(OAuthTokenRequest request) throws OAuthProblemException {
        if (!storage.resourceOwnerExists(request.getUsername(), request.getPassword())) {
            String message = String.format("No resource owner with name %s registered or invalid password provided",
                    request.getUsername());
            throw OAuthProblemException.error(message);
        }
    }
}
