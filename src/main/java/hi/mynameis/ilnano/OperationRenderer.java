package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.AnnotationSpec;
import io.micronaut.sourcegen.javapoet.CodeBlock;
import io.micronaut.sourcegen.javapoet.MethodSpec;
import io.micronaut.sourcegen.javapoet.ParameterSpec;
import io.micronaut.sourcegen.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.List;

final class OperationRenderer {

    private final TypeMapper typeMapper;

    private final ParameterNamer parameterNamer;

    OperationRenderer(TypeMapper typeMapper) {
        this(typeMapper, new ParameterNamer());
    }

    OperationRenderer(TypeMapper typeMapper, ParameterNamer parameterNamer) {
        this.typeMapper = typeMapper;
        this.parameterNamer = parameterNamer;
    }

    MethodSpec render(OperationInfo operation) {
        return MethodSpec.methodBuilder(operation.operationId())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(operationAnnotation(operation))
                .returns(returnType(operation))
                .addParameters(operation.params().stream().map(this::renderParameter).toList())
                .build();
    }

    private AnnotationSpec operationAnnotation(OperationInfo operation) {
        var annotation = AnnotationSpec.builder(ApiAnnotation.class)
                .addMember("type", "$T.$L", OperationTypeEnum.class, operation.httpMethod())
                .addMember("path", "$S", operation.path());

        var mediaTypes = operation.response().mediaTypes();
        if (!mediaTypes.isEmpty()) {
            annotation.addMember("produces", producedMediaTypes(mediaTypes));
        }
        return annotation.build();
    }

    private CodeBlock producedMediaTypes(List<String> mediaTypes) {
        return mediaTypes.size() == 1
                ? CodeBlock.of("$S", mediaTypes.get(0))
                : mediaTypes.stream()
                        .map(mediaType -> CodeBlock.of("$S", mediaType))
                        .collect(CodeBlock.joining(", ", "{", "}"));
    }

    private TypeName returnType(OperationInfo operation) {
        return operation.response().schema() == null
                ? TypeName.VOID
                : typeMapper.toTypeName(operation.response().schema());
    }

    private ParameterSpec renderParameter(ParamInfo param) {
        return ParameterSpec.builder(
                        typeMapper.toTypeName(param.schema()),
                        parameterNamer.toJavaName(param.name()))
                .addAnnotation(AnnotationSpec.builder(ApiParam.class)
                        .addMember("name", "$S", param.name())
                        .addMember("in", "$T.$L", ParameterLocation.class, param.in())
                        .addMember("required", "$L", param.required())
                        .build())
                .build();
    }
}
