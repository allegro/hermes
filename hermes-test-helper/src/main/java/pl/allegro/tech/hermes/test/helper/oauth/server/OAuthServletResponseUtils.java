package pl.allegro.tech.hermes.test.helper.oauth.server;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class OAuthServletResponseUtils {

    static void sendResponse(HttpServletResponse resp, String body, int statusCode) throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(body);
        resp.getWriter().flush();
        resp.getWriter().close();
    }

    static OAuthResponse getOAuthJsonErrorResponse(OAuthProblemException cause, int statusCode) {
        try {
            return OAuthRSResponse.errorResponse(statusCode)
                    .error(cause)
                    .buildJSONMessage();
        } catch (OAuthSystemException e) {
            throw new RuntimeException(e);
        }
    }
}
