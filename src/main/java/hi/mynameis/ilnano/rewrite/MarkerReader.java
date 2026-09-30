package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;
import hi.mynameis.ilnano.ParameterLocation;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

/**
 * Turns the marker annotations on a generated interface into descriptors the flavours can render
 * from.
 */
final class MarkerReader {

    /**
     * The tag of an interface, present only when it carries {@code @ApiInterface}.
     */
    Optional<String> tagOf(J.ClassDeclaration api) {
        return MarkerAnnotation.find(api.getLeadingAnnotations(), ApiMarkers.API_INTERFACE)
                .flatMap(marker -> marker.string(ApiMarkers.TAG));
    }

    /**
     * The operation of a method, present only when it carries {@code @ApiAnnotation}.
     */
    Optional<OperationDescriptor> operationOf(J.MethodDeclaration method, Cursor cursor) {
        return MarkerAnnotation.find(method.getLeadingAnnotations(), ApiMarkers.API_ANNOTATION)
                .flatMap(marker -> describe(marker, method, cursor));
    }

    private Optional<OperationDescriptor> describe(
            MarkerAnnotation marker, J.MethodDeclaration method, Cursor cursor) {
        var httpMethod = marker.enumConstant(ApiMarkers.TYPE).flatMap(this::toHttpMethod);
        var path = marker.string(ApiMarkers.PATH);

        if (httpMethod.isEmpty() || path.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OperationDescriptor(
                httpMethod.get(),
                ResourcePath.of(path.get()),
                marker.strings(ApiMarkers.PRODUCES),
                parametersOf(method, cursor)));
    }

    private Optional<OperationTypeEnum> toHttpMethod(String constant) {
        try {
            return Optional.of(OperationTypeEnum.valueOf(constant));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private List<ParameterDescriptor> parametersOf(J.MethodDeclaration method, Cursor cursor) {
        return method.getParameters().stream()
                .map(parameter -> describeParameter(parameter, cursor))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * An empty parameter list is one {@link J.Empty} statement, and a parameter may in principle
     * lack the marker, so both are filtered out rather than assumed away.
     */
    private Optional<ParameterDescriptor> describeParameter(Statement parameter, Cursor cursor) {
        if (!(parameter instanceof J.VariableDeclarations declaration)
                || declaration.getTypeExpression() == null
                || declaration.getVariables().isEmpty()) {
            return Optional.empty();
        }

        var javaName = declaration.getVariables().get(0).getSimpleName();
        var typeSource = declaration.getTypeExpression().printTrimmed(cursor);
        var marker = MarkerAnnotation.find(declaration.getLeadingAnnotations(), ApiMarkers.API_PARAM);

        return marker.map(found -> new ParameterDescriptor(
                found.string(ApiMarkers.NAME).orElse(javaName),
                javaName,
                typeSource,
                found.enumConstant(ApiMarkers.IN)
                        .flatMap(this::toLocation)
                        .orElse(ParameterLocation.QUERY),
                found.flag(ApiMarkers.REQUIRED)));
    }

    private Optional<ParameterLocation> toLocation(String constant) {
        try {
            return Optional.of(ParameterLocation.valueOf(constant));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
