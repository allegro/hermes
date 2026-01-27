package pl.allegro.tech.hermes.management.infrastructure.console;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class FrontendRoutesFilter extends OncePerRequestFilter {
  private static final String FRONTEND_ENDPOINT = "/";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getRequestURI().startsWith("/ui")) {
      RequestDispatcher rd = request.getRequestDispatcher(FRONTEND_ENDPOINT);
      rd.forward(request, response);
    } else {
      filterChain.doFilter(request, response);
    }
  }
}
