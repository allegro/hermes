package pl.allegro.tech.hermes.common.ssl.provided;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

import static com.google.common.base.Strings.isNullOrEmpty;

interface ResourceLoader {
    default InputStream getResourceAsInputStream(URI location) throws FileNotFoundException {
        if ("classpath".equalsIgnoreCase(location.getScheme())) {
            return getClass().getClassLoader().getResourceAsStream(location.getSchemeSpecificPart());
        }
        return new FileInputStream(isNullOrEmpty(location.getPath()) ? location.getSchemeSpecificPart() : location.getPath());
    }
}
