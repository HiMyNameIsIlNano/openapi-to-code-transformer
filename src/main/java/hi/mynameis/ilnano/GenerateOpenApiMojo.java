package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Objects;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public final class GenerateOpenApiMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.sourceDirectory}/resources/openapi.yaml", property = "specPath")
    private String specPath;

    @Parameter(property = "specUrl")
    private String specUrl;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputFolder")
    private String outputFolder;

    public GenerateOpenApiMojo() {
        // Do not remove. For instantiation.
    }

    GenerateOpenApiMojo(String specPath, String specUrl, String outputFolder) {
        this.specPath = specPath;
        this.specUrl = specUrl;
        this.outputFolder = outputFolder;
    }

    public void execute() throws MojoExecutionException {
        if (!Objects.isNull(specPath) && !Objects.isNull(specUrl)) {
            throw new MojoExecutionException("specPath and specUrl cannot be set at the same time");
        }

        var openAPI = new OpenApiParser(new OpenAPIV3Parser()).parse(specPath);

        var write = new NoopWriter(outputFolder).write(new NoopOutput());

        Objects.requireNonNull(write);
    }

}
