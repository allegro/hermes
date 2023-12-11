package pl.allegro.tech.hermes.utils;

import org.springframework.http.HttpHeaders;

import java.util.Map;

public class Headers {
    public static HttpHeaders createHeaders(Map<String, String> map) {
        HttpHeaders headers = new HttpHeaders();
        map.forEach(headers::add);
        return headers;
    }
}
