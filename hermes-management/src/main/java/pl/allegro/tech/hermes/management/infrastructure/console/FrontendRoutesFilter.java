package pl.allegro.tech.hermes.management.infrastructure.console;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class FrontendRoutesFilter extends OncePerRequestFilter {
  private final String frontendEndpoint = "/";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getRequestURI().startsWith("/ui")) {
      RequestDispatcher rd = request.getRequestDispatcher(frontendEndpoint);
      rd.forward(request, response);
    } else {
      filterChain.doFilter(request, response);
    }
  }
}
