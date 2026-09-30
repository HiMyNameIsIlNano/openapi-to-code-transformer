package hi.mynameis.ilnano;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

public final class JavaSourceCompiler {

    /**
     * {@code @interface} is matched too, since a transformation may generate an annotation type.
     */
    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("public\\s+(?:@?interface|record|class|enum)\\s+(\\w+)");

    public List<String> compile(List<String> sources) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", Files.createTempDirectory("generated-classes").toString());

            compiler.getTask(null, fileManager, diagnostics, options, null, toFileObjects(sources))
                    .call();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compile generated sources", e);
        }

        return diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(Object::toString)
                .toList();
    }

    private List<JavaFileObject> toFileObjects(List<String> sources) {
        return sources.stream().map(this::inMemorySource).toList();
    }

    private JavaFileObject inMemorySource(String source) {
        var uri = URI.create("string:///" + declaredTypeName(source) + ".java");

        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
    }

    private String declaredTypeName(String source) {
        var matcher = TYPE_DECLARATION.matcher(source);

        if (!matcher.find()) {
            throw new AssertionError("no public type declared in generated source:\n" + source);
        }
        return matcher.group(1);
    }
}
