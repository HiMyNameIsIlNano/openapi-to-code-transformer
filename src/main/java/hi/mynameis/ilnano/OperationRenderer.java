package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.AnnotationSpec;
import io.micronaut.sourcegen.javapoet.MethodSpec;
import io.micronaut.sourcegen.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;

final class OperationRenderer {

    private final String modelPackage;

    OperationRenderer(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public MethodSpec render(OperationInfo operationInfo) {
        AnnotationSpec marker = AnnotationSpec.builder(ApiOperationMarker.class)
                .addMember("type", "$T.$L", Type.class, operationInfo.httpMethod())
                .addMember("path", "$S", operationInfo.path())
                .build();

        MethodSpec.Builder method = MethodSpec.methodBuilder(operationInfo.operationId())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT) // interface method
                .addAnnotation(marker)
                //.returns(TypeMapper.toTypeName(operationInfo.returnType(), modelPkg));
                .returns(TypeMapper.toTypeName(null, modelPackage));

        for (ParamInfo paramInfo : operationInfo.params()) {
            method.addParameter(
                    ParameterSpec.builder(
                                    TypeMapper.toTypeName(null, modelPackage),
                                    safeName(paramInfo.name()))
                            .build());
        }

        return method.build();
    }

    private String safeName(String name) {
        return name;
    }

}
