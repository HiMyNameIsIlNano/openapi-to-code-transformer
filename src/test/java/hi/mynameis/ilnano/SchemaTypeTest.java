package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

final class SchemaTypeTest {

    @ParameterizedTest
    @CsvSource({
            "integer, INTEGER",
            "number, NUMBER",
            "boolean, BOOLEAN",
            "string, STRING",
            "array, ARRAY",
            "object, OBJECT"
    })
    void resolves_every_openapi_type_keyword(String keyword, SchemaType expected) {
        assertThat(SchemaType.from(keyword)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nonsense", "Integer", "INTEGER"})
    void unknown_or_absent_keyword_falls_back_to_object(String keyword) {
        assertThat(SchemaType.from(keyword)).isEqualTo(SchemaType.OBJECT);
    }

    @ParameterizedTest
    @EnumSource(SchemaType.class)
    void keyword_round_trips_through_from(SchemaType type) {
        assertThat(SchemaType.from(type.keyword())).isEqualTo(type);
    }

    @Test
    void only_array_is_recognised_as_a_collection() {
        assertThat(SchemaType.ARRAY.isArray()).isTrue();
        assertThat(SchemaType.OBJECT.isArray()).isFalse();
        assertThat(SchemaType.STRING.isArray()).isFalse();
    }
}
