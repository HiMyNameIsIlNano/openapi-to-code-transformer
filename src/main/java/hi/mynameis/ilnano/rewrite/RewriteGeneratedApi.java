package hi.mynameis.ilnano.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replaces the generator's marker annotations with a {@link ClientFlavour}'s framework annotations.
 *
 * <p>The traversal is:
 *
 * <ol>
 *   <li>on an interface carrying {@code @ApiInterface}, work out the path prefix shared by every
 *       operation and remember it for the methods below;</li>
 *   <li>on each method carrying {@code @ApiAnnotation}, replace the leading annotations and rebuild
 *       the whole parameter list;</li>
 *   <li>replace the interface's own annotations last, so the methods have already been visited.</li>
 * </ol>
 *
 * A source without {@code @ApiInterface} is left untouched, which keeps the recipe safe to run over
 * a whole project rather than only the generated directory.
 */
final class RewriteGeneratedApi extends JavaIsoVisitor<ExecutionContext> {

    /**
     * Where {@link #visitClassDeclaration} leaves the prefix for {@link #visitMethodDeclaration} to
     * find on the cursor.
     */
    private static final String COMMON_PATH = "commonPath";

    private final ClientFlavour flavour;

    private final MarkerReader reader = new MarkerReader();

    RewriteGeneratedApi(ClientFlavour flavour) {
        this.flavour = flavour;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration api, ExecutionContext ctx) {
        var tag = reader.tagOf(api);

        if (tag.isEmpty()) {
            return api;
        }

        var commonPath = commonPathOf(api);
        getCursor().putMessage(COMMON_PATH, commonPath);

        var visited = super.visitClassDeclaration(api, ctx);

        // The template is applied against the tree the cursor holds, so the rewritten methods have
        // to be published to it first or they are discarded along with the old annotations.
        updateCursor(visited);
        removeMarkerImports();

        return replaceAnnotations(
                visited,
                flavour.interfaceAnnotations(tag.get(), commonPath),
                visited.getCoordinates().replaceAnnotations());
    }

    /**
     * The longest path prefix shared by every operation of the interface. With a single operation
     * that would be its entire path, which would leave the method with no path of its own and read
     * oddly, so a lone operation keeps its path on the method.
     */
    private ResourcePath commonPathOf(J.ClassDeclaration api) {
        var paths = operationPaths(api);

        return paths.size() < 2
                ? ResourcePath.of("")
                : paths.stream().reduce(ResourcePath::commonPrefixWith).orElseGet(() -> ResourcePath.of(""));
    }

    private List<ResourcePath> operationPaths(J.ClassDeclaration api) {
        return api.getBody().getStatements().stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .map(method -> reader.operationOf(method, getCursor()))
                .flatMap(Optional::stream)
                .map(OperationDescriptor::path)
                .toList();
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        var operation = reader.operationOf(method, getCursor());

        if (operation.isEmpty()) {
            return method;
        }

        var commonPath = getCursor().<ResourcePath>getNearestMessage(COMMON_PATH, ResourcePath.of(""));
        var rewritten = replaceParameters(method, operation.get());

        updateCursor(rewritten);

        return replaceAnnotations(
                rewritten,
                flavour.methodAnnotations(operation.get(), operation.get().path().relativeTo(commonPath)),
                rewritten.getCoordinates().replaceAnnotations());
    }

    /**
     * Rebuilds the parameter list in one go.
     *
     * <p>Replacing the annotations of an individual parameter through its own
     * {@code replaceAnnotations()} coordinates silently does nothing, so the list has to be
     * rewritten as a whole via the method's {@code replaceParameters()}.
     *
     * <p>The declared types are re-emitted as the source text they were printed from rather than as
     * template substitutions, because an untyped {@code #{}} substitution is rejected and a typed
     * one would need the model classes on the parse classpath.
     */
    private J.MethodDeclaration replaceParameters(
            J.MethodDeclaration method, OperationDescriptor operation) {
        if (operation.parameters().isEmpty()) {
            return method;
        }

        var declarations = operation.parameters().stream()
                .map(this::declarationOf)
                .toList();

        return applyTemplate(
                String.join(", ", declarations),
                method.getCoordinates().replaceParameters());
    }

    private String declarationOf(ParameterDescriptor parameter) {
        var annotations = flavour.parameterAnnotations(parameter);
        var prefix = annotations.isEmpty() ? "" : String.join(" ", annotations) + " ";

        return prefix + parameter.typeSource() + " " + parameter.javaName();
    }

    private <T extends J> T replaceAnnotations(
            T target, List<String> annotations, org.openrewrite.java.tree.JavaCoordinates at) {
        return annotations.isEmpty() ? target : applyTemplate(String.join("\n", annotations), at);
    }

    /**
     * Applies a template built from the flavour's source, registering the imports the inserted
     * annotations need in order to resolve. The template is context sensitive because the parameter
     * declarations refer to the model types imported by the source being rewritten.
     *
     * <p>Only the imports the generated code actually mentions are registered. Registering the
     * flavour's whole list would make the source exceed the star-import threshold and collapse a
     * package's worth of annotations into a wildcard.
     */
    private <T extends J> T applyTemplate(String code, org.openrewrite.java.tree.JavaCoordinates at) {
        var imports = importsReferencedBy(code);
        var result = JavaTemplate.builder(code)
                .imports(imports.toArray(String[]::new))
                .contextSensitive()
                .build()
                .<T>apply(getCursor(), at);

        imports.forEach(type -> maybeAddImport(type, null, false));
        return result;
    }

    /**
     * The flavour's imports whose simple name appears as an annotation in {@code code}.
     */
    private List<String> importsReferencedBy(String code) {
        return flavour.imports().stream()
                .filter(type -> code.contains("@" + simpleNameOf(type)))
                .toList();
    }

    private String simpleNameOf(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    /**
     * Drops the imports of the markers and of the enums they referenced. They have {@code SOURCE}
     * retention, so leaving them behind would make the rewritten code depend on the generator at
     * compile time for no reason.
     */
    private void removeMarkerImports() {
        List.of(ApiMarkers.API_INTERFACE,
                        ApiMarkers.API_ANNOTATION,
                        ApiMarkers.API_PARAM,
                        ApiMarkers.OPERATION_TYPE_ENUM,
                        ApiMarkers.PARAMETER_LOCATION)
                .forEach(marker -> maybeRemoveImport(ApiMarkers.qualified(marker)));
    }

    /**
     * Whether a source declares an interface carrying {@code @ApiInterface}, used to decide whether
     * a source is one of the generator's APIs at all.
     */
    static boolean isGeneratedApi(J.ClassDeclaration declaration) {
        return MarkerAnnotation.find(declaration.getLeadingAnnotations(), ApiMarkers.API_INTERFACE)
                .isPresent();
    }

    /**
     * The statements of a class body that are methods, as a mutable list, for callers that need to
     * inspect them.
     */
    static List<J.MethodDeclaration> methodsOf(J.ClassDeclaration declaration) {
        var methods = new ArrayList<J.MethodDeclaration>();

        for (Statement statement : declaration.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method) {
                methods.add(method);
            }
        }
        return methods;
    }
}
