package pl.allegro.tech.hermes.management.infrastructure.console;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

public class FrontendRoutesFilter extends OncePerRequestFilter {

    Pattern pattern = Pattern.compile("^/(v2)+.*");
    String frontendEndpoint = "/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (pattern.matcher(request.getRequestURI()).matches()) {
            RequestDispatcher rd = request.getRequestDispatcher(frontendEndpoint);
            rd.forward(request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
