package hi.mynameis.ilnano;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ParameterLocation {

    PATH("path"),
    QUERY("query"),
    HEADER("header"),
    COOKIE("cookie"),
    BODY("body");

    private static final Map<String, ParameterLocation> BY_KEYWORD = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ParameterLocation::keyword, Function.identity()));

    private final String keyword;

    ParameterLocation(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    static ParameterLocation from(String keyword) {
        return Optional.ofNullable(keyword)
                .map(BY_KEYWORD::get)
                .orElse(QUERY);
    }
}
