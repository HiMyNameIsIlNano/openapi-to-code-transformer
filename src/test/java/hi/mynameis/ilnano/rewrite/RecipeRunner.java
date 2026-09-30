package hi.mynameis.ilnano.rewrite;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.util.List;
import java.util.Optional;

/**
 * Runs a recipe over a set of Java sources and returns the resulting sources.
 *
 * <p>Deliberately parses without the generator on the classpath, which is how a consuming project
 * sees the generated code: the markers have {@code SOURCE} retention, so their types do not resolve
 * and the recipe has to cope with that.
 */
final class RecipeRunner {

    /**
     * Every source after the recipe ran: the ones it changed, the ones it generated, and the
     * originals it left alone.
     *
     * <p>The untouched sources are part of the result because the models the interfaces refer to are
     * never rewritten, and a caller compiling the output needs them.
     */
    List<String> run(Recipe recipe, List<String> sources) {
        var ctx = new InMemoryExecutionContext(error -> {
            throw new IllegalStateException("Recipe failed", error);
        });

        var parsed = JavaParser.fromJavaVersion()
                .build()
                .parse(ctx, sources.toArray(String[]::new))
                .toList();

        var results = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset()
                .getAllResults();

        var rewritten = new java.util.LinkedHashMap<String, String>();
        parsed.forEach(source -> rewritten.put(pathOf(source), source.printAll()));

        results.forEach(result -> {
            Optional.ofNullable(result.getBefore()).ifPresent(before -> rewritten.remove(pathOf(before)));
            Optional.ofNullable(result.getAfter())
                    .ifPresent(after -> rewritten.put(pathOf(after), after.printAll()));
        });

        return List.copyOf(rewritten.values());
    }

    private String pathOf(SourceFile source) {
        return source.getSourcePath().toString();
    }

    /**
     * The single rewritten source whose name matches, failing when it is absent.
     */
    String sourceNamed(List<String> sources, String typeName) {
        return sources.stream()
                .filter(source -> source.contains("interface " + typeName)
                        || source.contains("class " + typeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no source declaring %s among:%n%s".formatted(typeName, sources)));
    }
}
