package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public final class GenerateOpenApiMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.sourceDirectory}/resources/openapi.yaml", property = "specPath")
    private String specPath;

    @Parameter(property = "specUrl")
    private String specUrl;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputFolder")
    private String outputFolder;

    private final OpenAPIV3Parser parser;

    public GenerateOpenApiMojo() {
        this(new OpenAPIV3Parser());
    }

    GenerateOpenApiMojo(OpenAPIV3Parser parser) {
        this.parser = parser;
    }

    void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    void setSpecUrl(String specUrl) {
        this.specUrl = specUrl;
    }

    void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void execute() throws MojoExecutionException {
        Predicate<String> isNotNull = path -> !Objects.isNull(path);
        if (isNotNull.test(specPath) && isNotNull.test(specUrl)) {
            throw new MojoExecutionException("specPath and specUrl cannot be set at the same time");
        }

        var location = Optional.ofNullable(specPath)
                .orElse(specUrl);

         var openAPI = new OpenApiParser(parser)
                .parse(location);

        var write = new NoopWriter(outputFolder).write(new NoopOutput());

        Objects.requireNonNull(write);
    }

}
