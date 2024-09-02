package pl.allegro.tech.hermes.utils;

import java.util.Map;
import org.springframework.http.HttpHeaders;

public class Headers {
  public static HttpHeaders createHeaders(Map<String, String> map) {
    HttpHeaders headers = new HttpHeaders();
    map.forEach(headers::add);
    return headers;
  }
}
