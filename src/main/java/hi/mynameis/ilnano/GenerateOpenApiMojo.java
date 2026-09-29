package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class GenerateOpenApiMojo extends AbstractMojo {

    static final String DEFAULT_OUTPUT_FOLDER = "generated-sources";

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/openapi.yaml", property = "specDefinition")
    private String specDefinition;

    /**
     * Where the generated sources are written. Defaults to
     * {@code ${project.build.directory}/generated-sources/<openapi file name>}.
     */
    @Parameter(property = "outputFolder")
    private File outputFolder;

    @Parameter(defaultValue = "hi.mynameis.ilnano.generated", property = "basePackage")
    private String basePackage = "hi.mynameis.ilnano.generated";

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private final OpenAPIV3Parser parser;

    public GenerateOpenApiMojo() {
        this(new OpenAPIV3Parser());
    }

    GenerateOpenApiMojo(OpenAPIV3Parser parser) {
        this.parser = parser;
    }

    void setSpecDefinition(String specDefinition) {
        this.specDefinition = specDefinition;
    }

    void setOutputFolder(Path outputFolder) {
        this.outputFolder = Objects.isNull(outputFolder) ? null : outputFolder.toFile();
    }

    void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    void setBuildDirectory(Path buildDirectory) {
        this.buildDirectory = Objects.isNull(buildDirectory) ? null : buildDirectory.toFile();
    }

    void setProject(MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (Objects.isNull(specDefinition)) {
            throw new MojoExecutionException("specDefinition must be set");
        }

        var spec = new OpenApiParser(parser).parse(specDefinition);
        var generated = new JavaGenerator().generate(spec, basePackage);
        var outputDirectory = outputDirectory();

        try {
            var written = new SourceFileWriter(outputDirectory).write(generated);

            getLog().info("Generated %d API interfaces and %d models (%d files) into %s"
                    .formatted(generated.apis().size(), generated.models().size(),
                            written.size(), outputDirectory));
        } catch (UncheckedIOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        registerAsSourceRoot(outputDirectory);
    }

    /**
     * The configured output folder, or the build directory plus the name of the OpenAPI document.
     */
    Path outputDirectory() {
        if (Objects.nonNull(outputFolder)) {
            return outputFolder.toPath();
        }

        return buildDirectory().resolve(DEFAULT_OUTPUT_FOLDER)
                .resolve(new SpecLocation(specDefinition).directoryName());
    }

    private Path buildDirectory() {
        return Optional.ofNullable(buildDirectory)
                .map(File::toPath)
                .orElseGet(() -> Path.of("target"));
    }

    private void registerAsSourceRoot(Path outputDirectory) {
        Optional.ofNullable(project)
                .ifPresent(maven -> maven.addCompileSourceRoot(outputDirectory.toString()));
    }
}
