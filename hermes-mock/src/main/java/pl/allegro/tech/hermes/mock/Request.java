package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class Request {
    private String url;
    private RequestMethod method;
    private byte[] body;
    private Map<String, String> headers;

    public Request(LoggedRequest loggedRequest) {
        this.url = loggedRequest.getUrl();
        this.method = loggedRequest.getMethod();
        this.body = loggedRequest.getBody();
        this.headers = loggedRequest.getAllHeaderKeys().stream()
                .collect(toMap(key -> key, loggedRequest::getHeader));
    }

    public String getUrl() {
        return url;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
