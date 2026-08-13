package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class OpenApiParserTest {

    @Test
    void do_not_accept_null() {
        // Arrange
        var testSubject = new OpenApiParser(new OpenAPIV3Parser());

        // Act & Assert
        assertThatThrownBy(() -> testSubject.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"src/test/resources/openapi.yaml", "src/test/resources/openapi.json"})
    void can_parse_file(String location) {
        // Arrange
        var testSubject = new OpenApiParser(new OpenAPIV3Parser());

        // Act & Assert
        assertThat(testSubject.parse(location))
                .isNotNull();
    }

    @Test
    void cannot_retrieve_file() {
        // Arrange
        var location = "https://url.to.my.file/openapi.yaml";

        var mock = mock(OpenAPIV3Parser.class);
        when(mock.readContents(eq(location), isNull(), any(ParseOptions.class)))
                .thenReturn(null);
        var testSubject = new OpenApiParser(mock);

        // Act & Assert
        assertThatThrownBy(() -> testSubject.parse(location))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load remote spec at " + location);
    }
}