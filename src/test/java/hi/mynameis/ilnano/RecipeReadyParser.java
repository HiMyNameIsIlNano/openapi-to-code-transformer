package hi.mynameis.ilnano;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;

import java.util.List;

final class RecipeReadyParser {

    List<SourceFile> parse(List<String> sources) {
        return JavaParser.fromJavaVersion()
                .classpath(JavaParser.runtimeClasspath())
                .build()
                .parse(new InMemoryExecutionContext(error -> {
                    throw new IllegalStateException("Generated source failed to parse", error);
                }), sources.toArray(String[]::new))
                .toList();
    }
}
