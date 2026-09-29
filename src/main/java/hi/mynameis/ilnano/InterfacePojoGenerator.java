package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.JavaFile;
import io.micronaut.sourcegen.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

final class InterfacePojoGenerator implements Renderer {

    private final String apiPkg;

    private final OperationRenderer operationRenderer;

    InterfacePojoGenerator(String apiPkg, OperationRenderer operationRenderer) {
        this.apiPkg = apiPkg;
        this.operationRenderer = operationRenderer;
    }

    @Override
    public String render(ApiTag tag, List<OperationInfo> operationInfoList) {
        TypeSpec.Builder api = TypeSpec.interfaceBuilder(tag + "Api")
                .addModifiers(Modifier.PUBLIC);

        for (OperationInfo operationInfo : operationInfoList) {
            api.addMethod(operationRenderer.render(operationInfo));
        }

        JavaFile javaFile = JavaFile.builder(apiPkg, api.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();

        return javaFile.toString();
    }
}
