package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;

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
        String apiPkg = basePackage + ".api";
        String modelPkg = basePackage + ".model";

        var byTag = tagMatcher.groupByTag(spec);

        return new GenerationResult(List.of(), List.of());
    }

}
