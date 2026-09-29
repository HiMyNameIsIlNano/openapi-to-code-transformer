package hi.mynameis.ilnano;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

enum SchemaType {

    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    STRING("string"),
    ARRAY("array"),
    OBJECT("object");

    private static final Map<String, SchemaType> BY_KEYWORD = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SchemaType::keyword, Function.identity()));

    private final String keyword;

    SchemaType(String keyword) {
        this.keyword = keyword;
    }

    String keyword() {
        return keyword;
    }

    boolean isArray() {
        return this == ARRAY;
    }

    static SchemaType from(String keyword) {
        return Optional.ofNullable(keyword)
                .map(BY_KEYWORD::get)
                .orElse(OBJECT);
    }
}
