package hi.mynameis.ilnano;

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Objects;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class GenerateOpenApiMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.sourceDirectory}/resources/openapi.yaml", property = "specDefinition")
    private String specDefinition;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputFolder")
    private String outputFolder;

    @Parameter(defaultValue = "hi.mynameis.ilnano.generated", property = "basePackage")
    private String basePackage = "hi.mynameis.ilnano.generated";

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

    void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (Objects.isNull(specDefinition)) {
            throw new MojoExecutionException("specDefinition must be set");
        }

        var spec = new OpenApiParser(parser).parse(specDefinition);
        var generated = new JavaGenerator().generate(spec, basePackage);

        getLog().info("Generated %d API interfaces and %d models into %s"
                .formatted(generated.apis().size(), generated.models().size(), outputFolder));
    }
}
