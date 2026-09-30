package hi.mynameis.ilnano;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs a transformation's recipe over the generated sources, entirely in memory.
 *
 * <p>The marker-annotated sources are never written to disk: they carry annotations with
 * {@code SOURCE} retention, so compiling them would make the consuming project depend on this plugin
 * for nothing. Only the rewritten result is handed back.
 */
final class TransformationRunner {

    /**
     * The sources after {@code recipe} ran: every source it changed, every source it generated, and
     * the untouched originals.
     *
     * <p>Sources are keyed by path rather than returned as a list, because a recipe may generate
     * files — the Spring configuration class, a custom JAX-RS verb annotation — whose path is not
     * derivable from the source text alone.
     */
    Map<String, String> run(Recipe recipe, GeneratedSources generated) {
        var ctx = new InMemoryExecutionContext(error -> {
            throw new IllegalStateException("Transformation failed", error);
        });

        var parsed = parse(generated, ctx);
        var results = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset()
                .getAllResults();

        return merge(parsed, results);
    }

    private List<SourceFile> parse(GeneratedSources generated, InMemoryExecutionContext ctx) {
        return JavaParser.fromJavaVersion()
                .build()
                .parse(ctx, generated.all().toArray(String[]::new))
                .toList();
    }

    /**
     * The recipe's output, with the sources it did not touch added back. A result with a
     * {@code null} {@code after} is a deletion, so it contributes nothing.
     */
    private Map<String, String> merge(List<SourceFile> parsed, List<Result> results) {
        var changed = results.stream()
                .map(Result::getAfter)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(this::pathOf, SourceFile::printAll, (first, second) -> second));

        var untouched = parsed.stream()
                .filter(source -> !wasVisited(source, results))
                .collect(Collectors.toMap(this::pathOf, SourceFile::printAll, (first, second) -> second));

        return Stream.concat(untouched.entrySet().stream(), changed.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second));
    }

    private boolean wasVisited(SourceFile source, List<Result> results) {
        return results.stream().anyMatch(result ->
                Objects.nonNull(result.getBefore())
                        && pathOf(result.getBefore()).equals(pathOf(source)));
    }

    private String pathOf(SourceFile source) {
        return source.getSourcePath().toString();
    }
}
