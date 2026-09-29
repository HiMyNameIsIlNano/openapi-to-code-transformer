package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;
import java.util.Optional;

final class JavaGenerator implements Generator {

    private static final String API_PACKAGE = "api";
    private static final String MODEL_PACKAGE = "model";

    private final TagMatcher tagMatcher;

    JavaGenerator() {
        this(new TagMatcher());
    }

    JavaGenerator(TagMatcher tagMatcher) {
        this.tagMatcher = tagMatcher;
    }

    @Override
    public GeneratedSources generate(OpenAPI spec, String basePackage) {
        var modelPackage = basePackage + "." + MODEL_PACKAGE;
        var typeMapper = new TypeMapper(modelPackage);
        var namer = new ParameterNamer();

        var interfaceGenerator = new InterfacePojoGenerator(
                basePackage + "." + API_PACKAGE, new OperationRenderer(typeMapper, namer));
        var apis = tagMatcher.groupByTag(spec).entrySet().stream()
                .map(byTag -> interfaceGenerator.render(byTag.getKey(), byTag.getValue()))
                .toList();

        var recordGenerator = new RecordGenerator(modelPackage, typeMapper, namer);
        var models = componentSchemas(spec).entrySet().stream()
                .map(schema -> recordGenerator.render(schema.getKey(), schema.getValue()))
                .toList();

        return new GeneratedSources(models, apis);
    }

    private Map<String, Schema> componentSchemas(OpenAPI spec) {
        return Optional.ofNullable(spec.getComponents())
                .map(Components::getSchemas)
                .orElseGet(Map::of);
    }
}
