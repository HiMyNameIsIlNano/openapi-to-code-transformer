package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateOpenApiMojoTest {

    private static final String SPEC = "src/test/resources/generation/annotated-api.yaml";
    private static final String BASE_PACKAGE = "com.acme.generated";

    @TempDir
    private Path buildDirectory;

    /**
     * A mojo with one Quarkus transformation, which is the smallest valid configuration.
     */
    private GenerateOpenApiMojo mojo() {
        return mojo(new Transformation("QUARKUS"));
    }

    private GenerateOpenApiMojo mojo(Transformation... transformations) {
        var mojo = new GenerateOpenApiMojo(new OpenAPIV3Parser());
        mojo.setBuildDirectory(buildDirectory);
        mojo.setBasePackage(BASE_PACKAGE);
        mojo.setTransformations(List.of(transformations));

        return mojo;
    }

    /**
     * The default output directory of a flavour: the spec's directory plus the flavour's name.
     */
    private Path outputFor(String flavour) {
        return buildDirectory.resolve("generated-sources").resolve("annotated-api").resolve(flavour);
    }

    private Stream<Path> javaFilesIn(Path directory) throws Exception {
        try (var found = Files.walk(directory)) {
            return found.filter(path -> path.toString().endsWith(".java")).toList().stream();
        }
    }

    private List<String> sourcesIn(Path directory) throws Exception {
        return javaFilesIn(directory).map(path -> {
            try {
                return Files.readString(path);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
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
    void fails_when_no_transformation_is_configured() {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);
        testSubject.setTransformations(List.of());

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("At least one <transformation>")
                .hasMessageContaining("QUARKUS");
    }

    @Test
    void fails_when_the_transformations_block_is_absent() {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);
        testSubject.setTransformations(null);

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("At least one <transformation>");
    }

    @Test
    void fails_when_a_transformation_has_no_flavour() {
        var testSubject = mojo(new Transformation());
        testSubject.setSpecDefinition(SPEC);

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("needs a <flavour>");
    }

    @Test
    void fails_when_the_flavour_is_not_known() {
        var testSubject = mojo(new Transformation("MICRONAUT"));
        testSubject.setSpecDefinition(SPEC);

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Unknown flavour 'MICRONAUT'");
    }

    @Test
    void fails_when_two_transformations_would_generate_into_the_same_package() {
        var testSubject = mojo(new Transformation("QUARKUS"), new Transformation("SPRING"));
        testSubject.setSpecDefinition(SPEC);

        assertThatThrownBy(testSubject::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("both generate into package")
                .hasMessageContaining(BASE_PACKAGE);
    }

    @Test
    void the_flavour_is_accepted_in_any_capitalisation() {
        var testSubject = mojo(new Transformation("quarkus"));
        testSubject.setSpecDefinition(SPEC);

        assertThatNoException().isThrownBy(testSubject::execute);
    }

    @Test
    void writes_into_generated_sources_named_after_the_spec_by_default() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(outputFor("quarkus")).isDirectory();
        assertThat(javaFilesIn(outputFor("quarkus"))).isNotEmpty();
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

        assertThat(custom.resolve("quarkus")).isDirectory();
        assertThat(javaFilesIn(custom)).isNotEmpty();
        assertThat(buildDirectory.resolve("generated-sources")).doesNotExist();
    }

    @Test
    void a_transformation_can_override_the_output_folder() throws Exception {
        var custom = buildDirectory.resolve("just-here");
        var transformation = new Transformation("QUARKUS");
        transformation.setOutputFolder(custom);

        var testSubject = mojo(transformation);
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(custom.resolve("com/acme/generated/api/PetsApi.java")).isRegularFile();
    }

    @Test
    void lays_out_the_generated_files_by_package() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        var output = outputFor("quarkus");
        assertThat(output.resolve("com/acme/generated/api/PetsApi.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/api/OwnersApi.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/model/Pet.java")).isRegularFile();
        assertThat(output.resolve("com/acme/generated/model/Owner.java")).isRegularFile();
    }

    /**
     * The markers have SOURCE retention and would drag this plugin onto the consuming project's
     * compile path, so only the rewritten sources may be written.
     */
    @Test
    void the_written_sources_carry_no_marker_annotations() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(sourcesIn(outputFor("quarkus")))
                .isNotEmpty()
                .allSatisfy(source -> assertThat(source)
                        .doesNotContain("@ApiInterface")
                        .doesNotContain("@ApiAnnotation")
                        .doesNotContain("@ApiParam")
                        .doesNotContain("hi.mynameis.ilnano"));
    }

    @Test
    void the_quarkus_flavour_writes_jakarta_annotations() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(Files.readString(outputFor("quarkus").resolve("com/acme/generated/api/PetsApi.java")))
                .contains("@RegisterRestClient(configKey = \"pets-api\")")
                .contains("import jakarta.ws.rs.")
                .contains("@GET");
    }

    @Test
    void the_spring_flavour_writes_http_exchange_annotations_and_a_config_class() throws Exception {
        var testSubject = mojo(new Transformation("SPRING"));
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        var output = outputFor("spring");
        assertThat(Files.readString(output.resolve("com/acme/generated/api/PetsApi.java")))
                .contains("@HttpExchange")
                .contains("import org.springframework.web.service.annotation.");
        assertThat(output.resolve("com/acme/generated/api/HttpServiceClientConfig.java"))
                .isRegularFile();
    }

    @Test
    void both_flavours_can_be_generated_when_they_use_different_packages() throws Exception {
        var quarkus = new Transformation("QUARKUS");
        quarkus.setBasePackage("com.acme.quarkus");
        var spring = new Transformation("SPRING");
        spring.setBasePackage("com.acme.spring");

        var testSubject = mojo(quarkus, spring);
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(outputFor("quarkus").resolve("com/acme/quarkus/api/PetsApi.java")).isRegularFile();
        assertThat(outputFor("spring").resolve("com/acme/spring/api/PetsApi.java")).isRegularFile();
    }

    @Test
    void written_sources_are_compilable_java() throws Exception {
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);

        testSubject.execute();

        assertThat(new JavaSourceCompiler().compile(sourcesIn(outputFor("quarkus")))).isEmpty();
    }

    @Test
    void registers_the_output_directory_as_a_compile_source_root() throws Exception {
        var project = new MavenProject();
        var testSubject = mojo();
        testSubject.setSpecDefinition(SPEC);
        testSubject.setProject(project);

        testSubject.execute();

        assertThat(project.getCompileSourceRoots())
                .contains(outputFor("quarkus").toString());
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
