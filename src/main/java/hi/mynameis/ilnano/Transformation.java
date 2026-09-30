package hi.mynameis.ilnano;

import org.apache.maven.plugins.annotations.Parameter;
import org.openrewrite.Recipe;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * One entry of the mojo's {@code <transformations>} block: a framework to rewrite the generated
 * sources into, and optionally where to put the result.
 *
 * <p>Maven instantiates this reflectively and sets the fields directly, so it needs a no-argument
 * constructor and must not be a record. The setters exist for the tests.
 */
public final class Transformation {

    /**
     * Which framework to target. Mandatory: a transformation with no flavour has nothing to do.
     */
    @Parameter(required = true)
    private String flavour;

    /**
     * The root package of the transformed sources, defaulting to the mojo's {@code basePackage}.
     * Two transformations in the same execution must not share one, since both flavours declare the
     * same class names.
     */
    @Parameter
    private String basePackage;

    /**
     * Where the transformed sources go, defaulting to a directory named after the flavour below the
     * mojo's output folder.
     */
    @Parameter
    private File outputFolder;

    /**
     * The {@code @ImportHttpServices} group, and therefore the key the base URL is configured
     * under. Only meaningful for {@link TransformationFlavour#SPRING}.
     */
    @Parameter
    private String group;

    public Transformation() {
    }

    Transformation(String flavour) {
        this.flavour = flavour;
    }

    void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    void setOutputFolder(Path outputFolder) {
        this.outputFolder = Objects.isNull(outputFolder) ? null : outputFolder.toFile();
    }

    void setGroup(String group) {
        this.group = group;
    }

    /**
     * The configured flavour.
     *
     * @throws IllegalArgumentException when it is missing or not one of the known values
     */
    TransformationFlavour flavour() {
        if (Objects.isNull(flavour) || flavour.isBlank()) {
            throw new IllegalArgumentException(
                    "Every <transformation> needs a <flavour>. Valid values are %s."
                            .formatted(TransformationFlavour.valid()));
        }
        return TransformationFlavour.from(flavour);
    }

    /**
     * The package to generate into, falling back to the mojo's.
     */
    String basePackage(String fallback) {
        return Optional.ofNullable(basePackage)
                .filter(configured -> !configured.isBlank())
                .orElse(fallback);
    }

    /**
     * The directory to write to, falling back to a subdirectory of {@code fallback} named after the
     * flavour. Keeping the flavours in separate directories is what lets several of them be
     * generated from one spec without overwriting each other.
     */
    Path outputDirectory(Path fallback) {
        return Optional.ofNullable(outputFolder)
                .map(File::toPath)
                .orElseGet(() -> fallback.resolve(flavour().name().toLowerCase(java.util.Locale.ROOT)));
    }

    /**
     * The recipe for this transformation, with the group applied when the flavour supports one.
     */
    Recipe recipe() {
        var resolved = flavour();

        return resolved == TransformationFlavour.SPRING && Objects.nonNull(group) && !group.isBlank()
                ? hi.mynameis.ilnano.rewrite.ClientRecipes.spring(group)
                : resolved.recipe();
    }

    @Override
    public String toString() {
        return "transformation[flavour=%s]".formatted(flavour);
    }
}
