package hi.mynameis.ilnano;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

final class ParameterLocationTest {

    @ParameterizedTest
    @CsvSource({
            "path, PATH",
            "query, QUERY",
            "header, HEADER",
            "cookie, COOKIE",
            "body, BODY"
    })
    void resolves_every_parameter_location_keyword(String keyword, ParameterLocation expected) {
        assertThat(ParameterLocation.from(keyword)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"formData", "PATH", "matrix"})
    void unknown_or_absent_location_falls_back_to_query(String keyword) {
        assertThat(ParameterLocation.from(keyword)).isEqualTo(ParameterLocation.QUERY);
    }

    @ParameterizedTest
    @EnumSource(ParameterLocation.class)
    void keyword_round_trips_through_from(ParameterLocation location) {
        assertThat(ParameterLocation.from(location.keyword())).isEqualTo(location);
    }
}
