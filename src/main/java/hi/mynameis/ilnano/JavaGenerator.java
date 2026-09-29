package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;

final class JavaGenerator implements Generator {

    private final TagMatcher tagMatcher;

    public JavaGenerator() {
        this(new TagMatcher());
    }

    JavaGenerator(TagMatcher tagMatcher) {
        this.tagMatcher = tagMatcher;
    }

    @Override
    public GenerationResult generate(OpenAPI spec, String basePackage) {
        var byTag = tagMatcher.groupByTag(spec);

        var operationRenderer = new OperationRenderer(basePackage + ".model");
        var interfacePojoGenerator = new InterfacePojoGenerator(basePackage + ".api", operationRenderer);
        var apiSources = byTag.entrySet().stream()
                .map(apiTagListEntry -> interfacePojoGenerator.render(apiTagListEntry.getKey(), apiTagListEntry.getValue()))
                .toList();

        var modelGenerator = new RecordGenerator();
        var modelSources = byTag.entrySet().stream()
                .map(apiTagListEntry -> modelGenerator.render(apiTagListEntry.getKey(), apiTagListEntry.getValue()))
                .toList();

        var parser = JavaParser.fromJavaVersion()
                .build();
        var context = new InMemoryExecutionContext();

        var apis = parser.parse(context, apiSources.toArray(new String[0]))
                .toList();
        var models = parser.parse(context, modelSources.toArray(new String[0]))
                .toList();

        return new GenerationResult(models, apis);
    }

}
