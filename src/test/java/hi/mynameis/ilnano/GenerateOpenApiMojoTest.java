package hi.mynameis.ilnano;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    @TempDir
    Path specFile;

    @TempDir
    Path outputDir;

    @Test
    void fails_when_both_spec_sources_provided() {
        var testSubject = new GenerateOpenApiMojo(specFile.toFile(),
                URI.create("https://x/spec.yaml"),
                outputDir.toFile());

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("specPath and specUrl cannot be set at the same time");
    }

    @Test
    void can_write_output_to_directory() {
        var testSubject = new GenerateOpenApiMojo(specFile.toFile(),
                null,
                outputDir.toFile());

        assertThatNoException().isThrownBy(testSubject::execute);
    }

}