package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    private GenerateOpenApiMojo mojo() {
        var mojo = new GenerateOpenApiMojo(new OpenAPIV3Parser());
        mojo.setOutputFolder("dummy");
        mojo.setBasePackage("com.acme.generated");

        return mojo;
    }

    @Test
    void fails_when_no_spec_is_provided() {
        var testSubject = mojo();

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("specDefinition must be set");
    }

    @Test
    void fails_when_the_spec_cannot_be_read() {
        var testSubject = mojo();
        testSubject.setSpecDefinition("https://x/spec.yaml");

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void can_write_output_to_directory() {
        var testSubject = mojo();
        testSubject.setSpecDefinition("src/test/resources/openapi.yaml");

        assertThatNoException().isThrownBy(testSubject::execute);
    }
}
