package hi.mynameis.ilnano;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.URI;
import java.util.Objects;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public final class GenerateOpenApiMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.sourceDirectory}/resources/openapi.yaml", property = "specPath")
    private File specPath;

    @Parameter(property = "specUrl")
    private URI specUrl;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputFolder")
    private File outputFolder;

    public GenerateOpenApiMojo() {
        // Do not remove. For instantiation.
    }

    GenerateOpenApiMojo(File specPath, URI specUrl, File outputFolder) {
        this.specPath = specPath;
        this.specUrl = specUrl;
        this.outputFolder = outputFolder;
    }

    public void execute() throws MojoExecutionException {
        if (!Objects.isNull(specPath) && !Objects.isNull(specUrl)) {
            throw new MojoExecutionException("specPath and specUrl cannot be set at the same time");
        }

        var write = new NoopWriter(outputFolder).write(new NoopOutput());

        Objects.requireNonNull(write);
    }

}
