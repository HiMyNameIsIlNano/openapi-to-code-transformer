package hi.mynameis.ilnano;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

final class ParameterNamerTest {

    private final ParameterNamer testSubject = new ParameterNamer();

    @ParameterizedTest
    @CsvSource({
            "petId, petId",
            "X-Trace-Id, xTraceId",
            "user_name, userName",
            "api.key, apiKey",
            "limit, limit"
    })
    void turns_spec_names_into_lower_camel_case_identifiers(String specName, String expected) {
        assertThat(testSubject.toJavaName(specName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "int", "return", "package"})
    void escapes_java_keywords(String keyword) {
        assertThat(testSubject.toJavaName(keyword)).isEqualTo(keyword + "Param");
    }

    @ParameterizedTest
    @CsvSource({"2fa, param2fa", "123, param123"})
    void prefixes_names_that_cannot_start_an_identifier(String specName, String expected) {
        assertThat(testSubject.toJavaName(specName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"'', param", "'-', param", "'--', param"})
    void falls_back_to_a_default_when_nothing_usable_remains(String specName, String expected) {
        assertThat(testSubject.toJavaName(specName)).isEqualTo(expected);
    }
}
