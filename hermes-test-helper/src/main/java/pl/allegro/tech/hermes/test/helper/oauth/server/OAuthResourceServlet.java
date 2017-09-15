package pl.allegro.tech.hermes.test.helper.oauth.server;

import io.undertow.util.Headers;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static pl.allegro.tech.hermes.test.helper.oauth.server.OAuthServletResponseUtils.getOAuthJsonErrorResponse;
import static pl.allegro.tech.hermes.test.helper.oauth.server.OAuthServletResponseUtils.sendResponse;

class OAuthResourceServlet extends HttpServlet {

    private final OAuthTestServerStorage storage;

    OAuthResourceServlet(OAuthTestServerStorage storage) {
        this.storage = storage;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            OAuthAccessResourceRequest request = new OAuthAccessResourceRequest(req);

            String owner = getResourceOwner(req);
            validateAccessToken(owner, request.getAccessToken());
            storage.incrementResourceAccessCount(owner);
            sendResponse(resp, "this is the secret of " + owner, 200);
        } catch (OAuthProblemException e) {
            OAuthResponse response = getOAuthJsonErrorResponse(e, HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");
            resp.setHeader(Headers.WWW_AUTHENTICATE.toString(), "Token");
            sendResponse(resp, response.getBody(), response.getResponseStatus());
        } catch (OAuthSystemException e) {
            sendResponse(resp, e.getMessage(), 500);
        }
    }

    private String getResourceOwner(HttpServletRequest req) {
        return Optional.ofNullable(req.getParameter("username"))
                .orElseGet(() -> req.getParameter("clientId"));
    }

    private void validateAccessToken(String owner, String token) throws OAuthProblemException {
        if (!storage.accessTokenExists(owner, token)) {
            throw OAuthProblemException.error("Invalid resource owner or access token");
        }
    }
}
