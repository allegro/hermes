package pl.allegro.tech.hermes.management.api;

import org.springframework.web.filter.GenericFilterBean;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

public class ReadOnlyFilter extends GenericFilterBean {
    private static final String READ_ONLY_ERROR_MESSAGE = "Action forbidden due to read-only mode";

    private final ModeService modeService;

    public ReadOnlyFilter(ModeService modeService) {
        this.modeService = modeService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (modeService.isReadOnlyEnabled()) {
            HttpServletRequest req = (HttpServletRequest) request;
            if (!req.getMethod().equals("GET") && !isWhitelisted(req.getRequestURI())) {
                HttpServletResponse resp = ((HttpServletResponse) response);
                resp.setStatus(SC_SERVICE_UNAVAILABLE);
                resp.getOutputStream().println(READ_ONLY_ERROR_MESSAGE);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isWhitelisted(String requestURI) {
        if (requestURI.startsWith("/query")) {
            return true;
        }
        if (requestURI.startsWith("/mode")) {
            return true;
        }
        if (requestURI.startsWith("/topics") && requestURI.endsWith("query")) {
            return true;
        }
        return false;
    }
}
