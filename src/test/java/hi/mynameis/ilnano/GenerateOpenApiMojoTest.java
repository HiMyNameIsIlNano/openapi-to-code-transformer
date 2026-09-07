package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    @Test
    void fails_when_both_spec_sources_provided() {
        // Arrange
        var testSubject = new GenerateOpenApiMojo(new OpenAPIV3Parser());

        testSubject.setSpecDefinition("src/test/resources/openapi.yaml");
        testSubject.setSpecDefinition("https://x/spec.yaml");
        testSubject.setOutputFolder("dummy");

        // Act & Assert
        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("specPath and specUrl cannot be set at the same time");
    }

    @Test
    void can_write_output_to_directory() {
        // Arrange
        var testSubject = new GenerateOpenApiMojo(new OpenAPIV3Parser());

        testSubject.setSpecDefinition("src/test/resources/openapi.yaml");
        testSubject.setOutputFolder("dummy");

        // Act & Assert
        assertThatNoException().isThrownBy(testSubject::execute);
    }

}