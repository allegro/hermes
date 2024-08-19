package pl.allegro.tech.hermes.consumers.uri;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public final class UriUtils {

    private UriUtils() {
    }

    public static String extractHostFromUri(URI uri) {
        // we are handling here bug from jdk http://bugs.java.com/view_bug.do?bug_id=6587184
        return Optional
            .ofNullable(uri.getHost())
            .orElseThrow(() -> new InvalidHostException("Host name contains invalid chars. Underscore is one of them."))
            .replace("/", "");
    }

    public static Integer extractPortFromUri(URI uri) {
        return uri.getPort() > 0 ? uri.getPort() : null;
    }

    public static String extractAddressFromUri(URI uri) {
        String address = extractHostFromUri(uri);
        if (uri.getPort() > 0) {
            address += ":" + uri.getPort();
        }
        return address;
    }

    public static String extractUserNameFromUri(URI uri) {
        if (uri.getRawUserInfo() == null) {
            return null;
        }
        List<String> userInfoParts = splitUserInfo(uri);
        if (userInfoParts.isEmpty()) {
            return null;
        }
        return userInfoParts.get(0);
    }

    public static String extractPasswordFromUri(URI uri) {
        if (uri.getRawUserInfo() == null) {
            return null;
        }
        List<String> userInfoParts = splitUserInfo(uri);
        if (userInfoParts.size() <= 1) {
            return null;
        }

        return userInfoParts.get(1);
    }

    public static String extractContextFromUri(URI uri) {
        return StringUtils.substringAfter(uri.toString(), uri.getAuthority());
    }

    public static String extractDestinationTopicFromUri(URI uri) {
        return uri.getPath().replace("/", "");
    }

    public static URI appendContext(URI uri, String context) {
        return URI.create(StringUtils.removeEnd(uri.toString(), "/")
                + "/"
                + StringUtils.removeStart(context, "/")
        );
    }

    private static List<String> splitUserInfo(URI uri) {
        return Splitter.on(":").splitToList(uri.getRawUserInfo());
    }

}
