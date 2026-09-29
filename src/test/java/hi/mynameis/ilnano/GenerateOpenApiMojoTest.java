package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    private static final String SPEC = "src/test/resources/generation/annotated-api.yaml";
    private static final String BASE_PACKAGE = "com.acme.generated";

    @TempDir
    private Path buildDirectory;

    private GenerateOpenApiMojo mojo() {
        var mojo = new GenerateOpenApiMojo(new OpenAPIV3Parser());
        mojo.setBuildDirectory(buildDirectory);
        mojo.setBasePackage(BASE_PACKAGE);

        return mojo;
    }

    private Stream<Path> javaFilesIn(Path directory) throws Exception {
        try (var found = Files.walk(directory)) {
            return found.filter(path -> path.toString().endsWith(".java")).toList().stream();
        }
    }

    @Test
    void fails_when_no_spec_is_provided() {
        var testSubject = mojo();
        testSubject.setSpecDefinition(null);

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
    void writes_into_generated_sources_named_after_the_spec_by_default() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        var expected = buildDirectory.resolve("generated-sources").resolve("annotated-api");
        assertThat(expected).isDirectory();
        assertThat(javaFilesIn(expected)).isNotEmpty();
    }

    @Test
    void default_output_directory_is_derived_without_running_the_generator() {
        var testSubject = mojo();
        testSubject.setSpecDefinition("src/main/resources/pet-store.yaml");

        assertThat(testSubject.outputDirectory())
                .isEqualTo(buildDirectory.resolve("generated-sources").resolve("pet-store"));
    }

    @Test
    void writes_into_the_configured_output_folder_when_one_is_given() throws Exception {
        var custom = buildDirectory.resolve("custom/api-sources");
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);
        testSubject.setOutputFolder(custom);

        testSubject.execute();

        assertThat(custom).isDirectory();
        assertThat(javaFilesIn(custom)).isNotEmpty();
        assertThat(buildDirectory.resolve("generated-sources")).doesNotExist();
    }

    @Test
    void lays_out_the_generated_files_by_package() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        var output = buildDirectory.resolve("generated-sources").resolve("annotated-api");
        assertThat(output.resolve("com/acme/generated/api/PetsApi.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/api/OwnersApi.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/model/Pet.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/model/Owner.java")).isRegularFile();
    }

    @Test
    void written_sources_are_compilable_java() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        var output = buildDirectory.resolve("generated-sources").resolve("annotated-api");
        var sources = javaFilesIn(output).map(path -> {
            try {
                return Files.readString(path);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();

        assertThat(new JavaSourceCompiler().compile(sources)).isEmpty();
    }

    @Test
    void registers_the_output_directory_as_a_compile_source_root() throws Exception {
        var project = new MavenProject();
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);
        testSubject.setProject(project);

        testSubject.execute();

        assertThat(project.getCompileSourceRoots())
                .contains(buildDirectory.resolve("generated-sources")
                        .resolve("annotated-api").toString());
    }

    @Test
    void can_be_executed_twice_without_failing() {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        assertThatNoException().isThrownBy(() -> {
            testSubject.execute();
            testSubject.execute();
        });
    }
}
