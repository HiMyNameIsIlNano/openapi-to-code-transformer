package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.JavaFile;
import io.micronaut.sourcegen.javapoet.ParameterSpec;
import io.micronaut.sourcegen.javapoet.TypeSpec;
import io.swagger.v3.oas.models.media.Schema;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Optional;

final class RecordGenerator implements ModelRenderer {

    private final String modelPackage;

    private final TypeMapper typeMapper;

    private final ParameterNamer componentNamer;

    RecordGenerator(String modelPackage, TypeMapper typeMapper, ParameterNamer componentNamer) {
        this.modelPackage = modelPackage;
        this.typeMapper = typeMapper;
        this.componentNamer = componentNamer;
    }

    @Override
    public String render(String schemaName, Schema<?> schema) {
        var model = TypeSpec.recordBuilder(schemaName)
                .addModifiers(Modifier.PUBLIC)
                .addRecordComponents(components(schema))
                .build();

        return JavaFile.builder(modelPackage, model)
                .skipJavaLangImports(true)
                .indent("    ")
                .build()
                .toString();
    }

    private Iterable<ParameterSpec> components(Schema<?> schema) {
        return properties(schema).entrySet().stream()
                .map(property -> ParameterSpec.builder(
                                typeMapper.toTypeName(property.getValue()),
                                componentNamer.toJavaName(property.getKey()))
                        .build())
                .toList();
    }

    private Map<String, Schema> properties(Schema<?> schema) {
        return Optional.ofNullable(schema.getProperties()).orElseGet(Map::of);
    }
}
