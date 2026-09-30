package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rewrites the generated marker-annotated interfaces into MicroProfile REST clients.
 *
 * <p>Suited to a Quarkus project using {@code quarkus-rest-client-jackson}. Each interface becomes
 * a {@code @RegisterRestClient} with a {@code configKey} derived from its tag, so the base URL is
 * configured as {@code quarkus.rest-client.<config-key>.url}.
 *
 * <p>Scans first because a verb {@code jakarta.ws.rs} has no annotation for needs one generated
 * alongside the interfaces that use it.
 */
public class QuarkusRestClientRecipe extends ScanningRecipe<QuarkusRestClientRecipe.CustomVerbs> {

    @Override
    public String getDisplayName() {
        return "Rewrite generated API interfaces into MicroProfile REST clients";
    }

    @Override
    public String getDescription() {
        return "Replaces the `@ApiInterface`, `@ApiAnnotation` and `@ApiParam` markers on the "
                + "generated interfaces with `jakarta.ws.rs` annotations and MicroProfile's "
                + "`@RegisterRestClient`, producing clients a Quarkus project can inject. "
                + "Interfaces without the markers are left alone.";
    }

    /**
     * The verbs needing a generated annotation, and the package to put each one in.
     */
    public static final class CustomVerbs {

        private final Map<OperationTypeEnum, String> packagesByVerb = new LinkedHashMap<>();

        void add(OperationTypeEnum verb, String apiPackage) {
            packagesByVerb.putIfAbsent(verb, apiPackage);
        }

        /**
         * Drops a verb whose annotation the sources already declare, so that a second run does not
         * generate it again.
         */
        void alreadyDeclared(String simpleName) {
            packagesByVerb.keySet().removeIf(verb -> CustomVerbAnnotation.nameOf(verb).equals(simpleName));
        }

        Map<OperationTypeEnum, String> all() {
            return Map.copyOf(packagesByVerb);
        }
    }

    @Override
    public CustomVerbs getInitialValue(ExecutionContext ctx) {
        return new CustomVerbs();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(CustomVerbs acc) {
        var reader = new MarkerReader();

        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, ExecutionContext ctx) {
                if (!RewriteGeneratedApi.isGeneratedApi(declaration)) {
                    // An annotation of the same name already in the sources must not be duplicated.
                    acc.alreadyDeclared(declaration.getSimpleName());
                    return declaration;
                }

                var operations = RewriteGeneratedApi.methodsOf(declaration).stream()
                        .map(method -> reader.operationOf(method, getCursor()))
                        .flatMap(Optional::stream)
                        .toList();

                packageOf(getCursor().firstEnclosing(J.CompilationUnit.class)).ifPresent(apiPackage ->
                        QuarkusFlavour.customVerbsOf(operations)
                                .forEach(verb -> acc.add(verb, apiPackage)));

                return declaration;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(CustomVerbs acc, ExecutionContext ctx) {
        return acc.all().entrySet().stream()
                .map(verb -> annotationSource(verb.getKey(), verb.getValue(), ctx))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private Optional<SourceFile> annotationSource(
            OperationTypeEnum verb, String apiPackage, ExecutionContext ctx) {
        var sourcePath = Path.of(
                apiPackage.replace('.', '/'), CustomVerbAnnotation.nameOf(verb) + ".java");

        return JavaParser.fromJavaVersion()
                .build()
                .parse(ctx, CustomVerbAnnotation.sourceOf(verb, apiPackage))
                .map(source -> (SourceFile) source.withSourcePath(sourcePath))
                .findFirst();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(CustomVerbs acc) {
        return new RewriteGeneratedApi(new QuarkusFlavour());
    }

    private static Optional<String> packageOf(J.CompilationUnit source) {
        return Optional.ofNullable(source)
                .map(J.CompilationUnit::getPackageDeclaration)
                .map(declaration -> declaration.getExpression().printTrimmed());
    }
}
