package hi.mynameis.ilnano;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The location of an OpenAPI document, either a file path or a URL.
 */
record SpecLocation(String value) {

    private static final String DEFAULT_DIRECTORY_NAME = "openapi";

    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");

    SpecLocation {
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * The name of the directory the sources generated from this document are written to,
     * derived from the document file name without its extension.
     */
    String directoryName() {
        var candidate = UNSAFE_CHARACTERS
                .matcher(withoutExtension(lastSegment(withoutQuery(value))))
                .replaceAll("_");

        return isUsable(candidate) ? candidate : DEFAULT_DIRECTORY_NAME;
    }

    private boolean isUsable(String candidate) {
        return !candidate.isBlank();
    }

    private String withoutQuery(String location) {
        return location.split("[?#]", 2)[0];
    }

    private String lastSegment(String location) {
        var separator = Math.max(location.lastIndexOf('/'), location.lastIndexOf('\\'));

        return location.substring(separator + 1);
    }

    private String withoutExtension(String fileName) {
        var extension = fileName.lastIndexOf('.');

        return extension < 0 ? fileName : fileName.substring(0, extension);
    }
}
