package hi.mynameis.ilnano;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Writes generated Java sources below an output directory, mirroring their package structure.
 */
final class SourceFileWriter implements ResultWriter<GeneratedSources, List<Path>> {

    private static final Pattern PACKAGE_DECLARATION =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("public\\s+(?:interface|record|class|enum)\\s+(\\w+)");

    private static final String JAVA_EXTENSION = ".java";

    private final Path outputDirectory;

    SourceFileWriter(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }

    @Override
    public List<Path> write(GeneratedSources data) {
        Objects.requireNonNull(data, "data must not be null");

        return data.all().stream().map(this::writeSource).toList();
    }

    private Path writeSource(String source) {
        var target = outputDirectory.resolve(relativePath(source));

        try {
            Files.createDirectories(target.getParent());

            return Files.writeString(target, source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write generated source to " + target, e);
        }
    }

    private Path relativePath(String source) {
        return packageDirectory(source).resolve(declaredTypeName(source) + JAVA_EXTENSION);
    }

    private Path packageDirectory(String source) {
        var matcher = PACKAGE_DECLARATION.matcher(source);

        return matcher.find() ? Path.of(matcher.group(1).replace('.', '/')) : Path.of("");
    }

    private String declaredTypeName(String source) {
        var matcher = TYPE_DECLARATION.matcher(source);

        if (!matcher.find()) {
            throw new IllegalArgumentException("No public type declared in generated source:\n" + source);
        }
        return matcher.group(1);
    }
}
