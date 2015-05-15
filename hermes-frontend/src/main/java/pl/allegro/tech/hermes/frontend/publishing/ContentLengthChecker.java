package pl.allegro.tech.hermes.frontend.publishing;

import javax.servlet.http.HttpServletRequest;

import static java.lang.String.format;

public final class ContentLengthChecker {
    private ContentLengthChecker() {
    }

    public static void checkContentLength(HttpServletRequest request, int contentLength, String message) {
        int expected = request.getContentLength();
        if (!isChunked(request) && expected != contentLength) {
            throw new IllegalStateException(format("%s [header:%s, actual:%s].", message, expected, contentLength));
        }
    }

    private static boolean isChunked(HttpServletRequest request) {
        return "chunked".equals(request.getHeader("Transfer-Encoding")) && request.getContentLength() < 0;
    }
}
