package hi.mynameis.ilnano;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

final class SchemaFormatTest {

    @ParameterizedTest
    @CsvSource({
            "int32, INT32",
            "int64, INT64",
            "float, FLOAT",
            "double, DOUBLE",
            "date, DATE",
            "date-time, DATE_TIME",
            "uuid, UUID",
            "binary, BINARY"
    })
    void resolves_every_known_format_keyword(String keyword, SchemaFormat expected) {
        assertThat(SchemaFormat.from(keyword)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"int128", "DATE-TIME", "guid"})
    void unknown_or_absent_format_falls_back_to_none(String keyword) {
        assertThat(SchemaFormat.from(keyword)).isEqualTo(SchemaFormat.NONE);
    }

    @ParameterizedTest
    @EnumSource(value = SchemaFormat.class, mode = EnumSource.Mode.EXCLUDE, names = "NONE")
    void keyword_round_trips_through_from(SchemaFormat format) {
        assertThat(SchemaFormat.from(format.keyword())).isEqualTo(format);
    }
}
