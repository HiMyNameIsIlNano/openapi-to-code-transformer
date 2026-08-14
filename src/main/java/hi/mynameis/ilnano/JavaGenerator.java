package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class JavaGenerator implements Generator {

    @Override
    public GenerationResult generate(OpenAPI spec, String basePackage) {
        String apiPkg = basePackage + ".api";
        String modelPkg = basePackage + ".model";

        // group operations by tag → one interface per tag
        Map<String, List<OperationInfo>> byTag = groupByTag(spec);

        return new GenerationResult(List.of(), List.of());
    }

    Map<String, List<OperationInfo>> groupByTag(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPi must not be null");

        var paths = Optional.ofNullable(openAPI.getPaths())
                .orElse(new Paths());

        return paths.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> toKey(entry.getKey()), entry -> toOperationInfo(entry.getValue())));
    }

    private String toKey(String input) {
        Objects.requireNonNull(input, "input must not be null");

        if (!input.matches("^/[a-zA-Z]+$")) {
            throw new IllegalArgumentException("Input must be in the form /letters (e.g. /pets), but was: " + input);
        }

        var path = input.substring(1);
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private List<OperationInfo> toOperationInfo(PathItem value) {
        return value.readOperationsMap()
                .entrySet()
                .stream()
                .map(httpMethodOperationEntry -> new OperationInfo(httpMethodOperationEntry.getKey().name(),
                        null, httpMethodOperationEntry.getValue().getOperationId(), null, null))
                .toList();
    }

}
