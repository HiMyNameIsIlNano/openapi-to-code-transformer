package hi.mynameis.ilnano;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

enum SchemaFormat {

    NONE(""),
    INT32("int32"),
    INT64("int64"),
    FLOAT("float"),
    DOUBLE("double"),
    DATE("date"),
    DATE_TIME("date-time"),
    UUID("uuid"),
    BINARY("binary");

    private static final Map<String, SchemaFormat> BY_KEYWORD = Arrays.stream(values())
            .filter(format -> format != NONE)
            .collect(Collectors.toUnmodifiableMap(SchemaFormat::keyword, Function.identity()));

    private final String keyword;

    SchemaFormat(String keyword) {
        this.keyword = keyword;
    }

    String keyword() {
        return keyword;
    }

    static SchemaFormat from(String keyword) {
        return Optional.ofNullable(keyword)
                .map(BY_KEYWORD::get)
                .orElse(NONE);
    }
}
