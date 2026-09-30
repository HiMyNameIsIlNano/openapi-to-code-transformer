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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * What to rewrite the generated sources into. At least one {@code <transformation>} is required:
     * the intermediate marker-annotated sources are not useful on their own, so a build with no
     * transformation would produce nothing compilable.
     */
    @Parameter
    private List<Transformation> transformations = new ArrayList<>();

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private final OpenAPIV3Parser parser;

    private final TransformationRunner runner;

    public GenerateOpenApiMojo() {
        this(new OpenAPIV3Parser());
    }

    GenerateOpenApiMojo(OpenAPIV3Parser parser) {
        this.parser = parser;
        this.runner = new TransformationRunner();
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

    void setTransformations(List<Transformation> transformations) {
        this.transformations = transformations;
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

        var requested = validateTransformations();
        var spec = new OpenApiParser(parser).parse(specDefinition);
        var outputDirectory = outputDirectory();

        for (var transformation : requested) {
            apply(transformation, spec, outputDirectory);
        }
    }

    /**
     * The configured transformations, rejecting a block that is empty or that would have two
     * flavours writing the same classes.
     *
     * <p>The flavours all declare the same fully qualified names, so two transformations sharing a
     * package would produce duplicate classes on the compile path. That is caught here rather than
     * left to a confusing {@code javac} error.
     */
    private List<Transformation> validateTransformations() throws MojoExecutionException {
        if (Objects.isNull(transformations) || transformations.isEmpty()) {
            throw new MojoExecutionException(
                    "At least one <transformation> must be configured inside <transformations>. "
                            + "Valid flavours are %s.".formatted(TransformationFlavour.valid()));
        }

        var byPackage = new HashMap<String, Transformation>();

        for (var transformation : transformations) {
            var resolved = resolvedPackage(transformation);
            var clash = byPackage.put(resolved, transformation);

            if (Objects.nonNull(clash)) {
                throw new MojoExecutionException(
                        ("%s and %s both generate into package '%s'. Both flavours declare the same "
                                + "class names, so give each transformation its own <basePackage>.")
                                .formatted(clash, transformation, resolved));
            }
        }

        return List.copyOf(transformations);
    }

    private String resolvedPackage(Transformation transformation) throws MojoExecutionException {
        try {
            return transformation.basePackage(basePackage);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Generates the marker-annotated sources, rewrites them into the transformation's flavour and
     * writes only the result.
     */
    private void apply(Transformation transformation, io.swagger.v3.oas.models.OpenAPI spec, Path outputDirectory)
            throws MojoExecutionException {
        Map<String, String> transformed;
        Path target;

        try {
            var generated = new JavaGenerator()
                    .generate(spec, transformation.basePackage(basePackage));

            transformed = runner.run(transformation.recipe(), generated);
            target = transformation.outputDirectory(outputDirectory);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new MojoExecutionException(
                    "Failed to apply %s: %s".formatted(transformation, e.getMessage()), e);
        }

        try {
            var written = new TransformedSourceWriter(target).write(transformed);

            getLog().info("Applied %s, writing %d files into %s"
                    .formatted(transformation, written.size(), target));
        } catch (UncheckedIOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        registerAsSourceRoot(target);
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
