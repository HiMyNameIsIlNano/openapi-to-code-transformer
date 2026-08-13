package hi.mynameis.ilnano;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    @Test
    void fails_when_both_spec_sources_provided() {
        var testSubject = new GenerateOpenApiMojo("src/test/resources/openapi.yaml",
                "https://x/spec.yaml",
                "dummy"
        );

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("specPath and specUrl cannot be set at the same time");
    }

    @Test
    void can_write_output_to_directory() {
        var testSubject = new GenerateOpenApiMojo("src/test/resources/openapi.yaml",
                null,
                "dummy"
        );

        assertThatNoException().isThrownBy(testSubject::execute);
    }

}