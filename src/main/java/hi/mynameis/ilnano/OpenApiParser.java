package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.Objects;
import java.util.Optional;

final class OpenApiParser implements Parser<OpenAPI> {

    private final SwaggerParserExtension openAPIV3Parser;

    public OpenApiParser(SwaggerParserExtension parser) {
        openAPIV3Parser = parser;
    }

    @Override
    public OpenAPI parse(String location) {
        Objects.requireNonNull(location);

        try {
            var result = openAPIV3Parser.readLocation(location, null, flatteningOptions());

            return Optional.ofNullable(result)
                    .map(SwaggerParseResult::getOpenAPI)
                    .orElseThrow(() -> new IllegalStateException("Failed to load remote spec at " + location));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ParseOptions flatteningOptions() {
        var options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setCamelCaseFlattenNaming(true);

        return options;
    }
}
