package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Supplier;

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

    @NullSource
    @ParameterizedTest
    void cannot_retrieve_null_file(SwaggerParseResult mockData) {
        // Arrange
        var location = "https://url.to.my.file/openapi.yaml";

        var mock = mock(OpenAPIV3Parser.class);
        when(mock.readContents(eq(location), isNull(), any(ParseOptions.class)))
                .thenReturn(mockData);
        var testSubject = new OpenApiParser(mock);

        // Act & Assert
        assertThatThrownBy(() -> testSubject.parse(location))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load remote spec at " + location);
    }

    @ParameterizedTest
    @MethodSource(value = "exceptionSupplier")
    void cannot_retrieve_file(Supplier<RuntimeException> supplier) {
        // Arrange
        var location = "https://url.to.my.file/openapi.yaml";

        var mock = mock(OpenAPIV3Parser.class);
        when(mock.readContents(eq(location), isNull(), any(ParseOptions.class)))
                .thenThrow(supplier.get());
        var testSubject = new OpenApiParser(mock);

        // Act & Assert
        assertThatThrownBy(() -> testSubject.parse(location))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load remote spec at " + location);
    }

    private static List<Supplier<RuntimeException>> exceptionSupplier() {
        return List.of(
                IllegalArgumentException::new,
                IllegalStateException::new
        );
    }

}