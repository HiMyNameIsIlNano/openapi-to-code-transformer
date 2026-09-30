package hi.mynameis.ilnano;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes transformed sources below an output directory, using the paths the recipe gave them.
 *
 * <p>Unlike {@link SourceFileWriter}, which derives the path from the {@code package} and type
 * declaration in the text, the path here comes from the recipe: a generated file such as the Spring
 * configuration class is written wherever the recipe put it.
 */
final class TransformedSourceWriter implements ResultWriter<Map<String, String>, List<Path>> {

    private final Path outputDirectory;

    TransformedSourceWriter(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }

    @Override
    public List<Path> write(Map<String, String> sources) {
        Objects.requireNonNull(sources, "sources must not be null");

        return sources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(source -> writeSource(source.getKey(), source.getValue()))
                .toList();
    }

    private Path writeSource(String relativePath, String source) {
        var target = outputDirectory.resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());

            return Files.writeString(target, source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write transformed source to " + target, e);
        }
    }
}
