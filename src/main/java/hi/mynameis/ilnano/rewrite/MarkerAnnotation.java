package hi.mynameis.ilnano.rewrite;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads the members of one of the generator's marker annotations.
 *
 * <p>The markers have {@code SOURCE} retention and the generator is only a {@code provided}
 * dependency of the projects that consume its output, so the annotations are usually not on the
 * parse classpath. That makes them arrive as {@link org.openrewrite.java.tree.JavaType.Unknown},
 * which rules out {@link org.openrewrite.java.tree.TypeUtils} and {@code AnnotationMatcher}: the
 * only thing that can be relied on is the simple name and the literal source of the members.
 */
final class MarkerAnnotation {

    private final J.Annotation annotation;

    private MarkerAnnotation(J.Annotation annotation) {
        this.annotation = annotation;
    }

    /**
     * The annotation on {@code annotations} whose simple name matches, if any.
     */
    static Optional<MarkerAnnotation> find(List<J.Annotation> annotations, String simpleName) {
        return annotations.stream()
                .filter(annotation -> simpleName.equals(annotation.getSimpleName()))
                .findFirst()
                .map(MarkerAnnotation::new);
    }

    /**
     * The value of a {@code String} member, for example {@code path = "/pets"}.
     */
    Optional<String> string(String member) {
        return literal(member)
                .map(J.Literal::getValue)
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    /**
     * The value of a {@code boolean} member, for example {@code required = true}.
     */
    boolean flag(String member) {
        return literal(member)
                .map(J.Literal::getValue)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    /**
     * The constant name of an enum member, for example {@code GET} for
     * {@code type = OperationTypeEnum.GET}. The generator always qualifies the constant, but a
     * hand-edited source may have static-imported it, hence the {@link J.Identifier} branch.
     */
    Optional<String> enumConstant(String member) {
        return member(member).map(this::simpleNameOf);
    }

    private String simpleNameOf(Expression value) {
        if (value instanceof J.FieldAccess fieldAccess) {
            return fieldAccess.getSimpleName();
        }
        return value instanceof J.Identifier identifier ? identifier.getSimpleName() : null;
    }

    /**
     * The values of a {@code String[]} member. Javapoet collapses a single-element array to a bare
     * literal ({@code produces = "application/json"}), so both shapes have to be handled.
     */
    List<String> strings(String member) {
        return member(member)
                .map(value -> value instanceof J.NewArray array
                        ? arrayElements(array)
                        : stringOf(value).map(List::of).orElseGet(List::of))
                .orElseGet(List::of);
    }

    private List<String> arrayElements(J.NewArray array) {
        return Optional.ofNullable(array.getInitializer())
                .orElseGet(List::of)
                .stream()
                .map(this::stringOf)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<String> stringOf(Expression value) {
        return Optional.of(value)
                .filter(J.Literal.class::isInstance)
                .map(J.Literal.class::cast)
                .map(J.Literal::getValue)
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    private Optional<J.Literal> literal(String member) {
        return member(member)
                .filter(J.Literal.class::isInstance)
                .map(J.Literal.class::cast);
    }

    /**
     * The right-hand side of {@code member = ...}.
     */
    private Optional<Expression> member(String member) {
        return Optional.ofNullable(annotation.getArguments())
                .orElseGet(List::of)
                .stream()
                .filter(J.Assignment.class::isInstance)
                .map(J.Assignment.class::cast)
                .filter(assignment -> isNamed(assignment, member))
                .map(J.Assignment::getAssignment)
                .findFirst();
    }

    private boolean isNamed(J.Assignment assignment, String member) {
        return assignment.getVariable() instanceof J.Identifier identifier
                && Objects.equals(member, identifier.getSimpleName());
    }
}
