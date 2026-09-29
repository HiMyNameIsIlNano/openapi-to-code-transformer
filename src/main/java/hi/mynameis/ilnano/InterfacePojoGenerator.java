package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.AnnotationSpec;
import io.micronaut.sourcegen.javapoet.JavaFile;
import io.micronaut.sourcegen.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

final class InterfacePojoGenerator implements Renderer {

    private static final String API_SUFFIX = "Api";

    private final String apiPackage;

    private final OperationRenderer operationRenderer;

    InterfacePojoGenerator(String apiPackage, OperationRenderer operationRenderer) {
        this.apiPackage = apiPackage;
        this.operationRenderer = operationRenderer;
    }

    @Override
    public String render(ApiTag tag, List<OperationInfo> operations) {
        var api = TypeSpec.interfaceBuilder(tag.value() + API_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ApiInterface.class)
                        .addMember("tag", "$S", tag.value())
                        .build())
                .addMethods(operations.stream().map(operationRenderer::render).toList())
                .build();

        return JavaFile.builder(apiPackage, api)
                .skipJavaLangImports(true)
                .indent("    ")
                .build()
                .toString();
    }
}
