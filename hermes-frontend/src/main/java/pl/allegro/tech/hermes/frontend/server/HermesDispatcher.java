package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.frontend.publishing.PublishingServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.regex.Pattern;

public class HermesDispatcher extends HttpServlet {

    private final PublishingServlet publishingServlet;
    private final HttpServlet notFound;
    private final Pattern publishingPattern;

    public HermesDispatcher(PublishingServlet publishingServlet, HttpServlet notFound, String topicsRoot) {
        this.publishingServlet = publishingServlet;
        this.notFound = notFound;
        this.publishingPattern = Pattern.compile(String.format("/%s/([^/]+)/?", topicsRoot));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        (isRequestForPublishing(req) ? publishingServlet : notFound).service(req, resp);
    }

    private boolean isRequestForPublishing(HttpServletRequest req) {
        return HttpMethod.POST.equals(req.getMethod()) && publishingPattern.matcher(req.getRequestURI()).matches();
    }
}
