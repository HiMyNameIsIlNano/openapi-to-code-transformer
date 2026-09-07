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

    @Parameter(defaultValue = "${project.build.sourceDirectory}/resources/openapi.yaml", property = "specDefinition")
    private String specDefinition;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputFolder")
    private String outputFolder;

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

    void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void execute() throws MojoExecutionException {
        if (Objects.isNull(specDefinition)) {
            throw new MojoExecutionException("specPath and specUrl cannot be set at the same time");
        }

         var openAPI = new OpenApiParser(parser)
                .parse(specDefinition);

        var write = new NoopWriter(outputFolder).write(new NoopOutput());

        Objects.requireNonNull(write);
    }

}
