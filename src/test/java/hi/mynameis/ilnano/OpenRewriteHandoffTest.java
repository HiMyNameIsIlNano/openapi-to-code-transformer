package hi.mynameis.ilnano;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class OpenRewriteHandoffTest implements OpenApiLoader {

    private static final String YAML_SPEC = "src/test/resources/generation/annotated-api.yaml";
    private static final String JSON_SPEC = "src/test/resources/generation/annotated-api.json";

    private final JavaGenerator generator = new JavaGenerator();

    private List<JavaSourceFile> parseApis(String spec) {
        var sources = generator.generate(load(spec), "com.acme.generated");

        return new RecipeReadyParser().parse(sources.all()).stream()
                .filter(JavaSourceFile.class::isInstance)
                .map(JavaSourceFile.class::cast)
                .toList();
    }

    private List<J.Annotation> annotationsOfType(String spec, Class<?> annotation) {
        var collected = new java.util.ArrayList<J.Annotation>();

        parseApis(spec).forEach(source -> new org.openrewrite.java.JavaIsoVisitor<Integer>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation visited, Integer context) {
                if (TypeUtils.isOfClassType(visited.getType(), annotation.getCanonicalName())) {
                    collected.add(visited);
                }
                return super.visitAnnotation(visited, context);
            }
        }.visit(source, 0));

        return collected;
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void generated_sources_parse_without_compilation_errors(String spec) {
        assertThat(parseApis(spec))
                .isNotEmpty()
                .allSatisfy(source -> assertThat(source.getClass().getSimpleName())
                        .isNotEqualTo("ParseError"));
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void every_operation_annotation_is_fully_type_attributed(String spec) {
        var annotations = annotationsOfType(spec, ApiAnnotation.class);

        assertThat(annotations).hasSize(5);
        assertThat(annotations).allSatisfy(annotation ->
                assertThat(TypeUtils.asFullyQualified(annotation.getType()))
                        .isNotNull()
                        .extracting(type -> type.getFullyQualifiedName())
                        .isEqualTo(ApiAnnotation.class.getCanonicalName()));
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void interface_annotation_is_type_attributed_once_per_tag(String spec) {
        assertThat(annotationsOfType(spec, ApiInterface.class)).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void parameter_annotations_are_type_attributed(String spec) {
        var annotations = annotationsOfType(spec, ApiParam.class);

        assertThat(annotations).hasSize(5);
        assertThat(annotations).allSatisfy(annotation ->
                assertThat(annotation.getType()).isNotNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void a_recipe_can_read_the_path_and_type_of_every_operation(String spec) {
        var arguments = annotationsOfType(spec, ApiAnnotation.class).stream()
                .map(annotation -> annotation.getArguments().stream()
                        .map(Object::toString)
                        .reduce("", String::concat))
                .toList();

        assertThat(arguments)
                .anySatisfy(argument -> assertThat(argument)
                        .contains("OperationTypeEnum.GET").contains("/pets/{petId}"))
                .anySatisfy(argument -> assertThat(argument)
                        .contains("OperationTypeEnum.DELETE").contains("/pets/{petId}"))
                .anySatisfy(argument -> assertThat(argument)
                        .contains("OperationTypeEnum.POST").contains("/pets"));
    }
}
