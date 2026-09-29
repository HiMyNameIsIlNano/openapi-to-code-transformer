package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.TypeName;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TagMatcher {

    private final TagMapper mapper;

    private final ReturnTypeResolver returnTypeResolver;

    TagMatcher() {
        this.mapper = new TagMapper(tag -> Character.toUpperCase(tag.charAt(0)) + tag.substring(1));
        this.returnTypeResolver = new ReturnTypeResolver();
    }

    public Map<ApiTag, List<OperationInfo>> groupByTag(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");

        var paths = Optional.ofNullable(openAPI.getPaths())
                .map(LinkedHashMap::entrySet)
                .orElse(Set.of());

        return paths.stream()
                .flatMap(this::toTaggedOperations)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList())
                ));
    }

    /**
     * It takes all the operations from the stream, group them by their ApiTag, and for each tag, collects the associated OperationInfo objects into an immutable list.
     *
     * @param pathEntry the entry containing the path (e.g. /api/foo) and its {@link PathItem}
     * @return the stream of {@link ApiTag} and {@link OperationInfo}
     * */
    private Stream<Entry<ApiTag, OperationInfo>> toTaggedOperations(Entry<String, PathItem> pathEntry) {
        var httpMethodToOperation = pathEntry.getValue()
                .readOperationsMap()
                .entrySet();
        
        return httpMethodToOperation.stream()
                .map(methodToOperation -> {
                    var operation = methodToOperation.getValue();
                    return Map.entry(
                            mapper.mapTags(operation.getTags()),
                            new OperationInfo(
                                    methodToOperation.getKey().name(),
                                    pathEntry.getKey(),
                                    operation.getOperationId(),
                                    returnTypeResolver.resolve(operation),
                                    resolveParams(operation)
                            )
                    );
                });
    }

    private List<ParamInfo> resolveParams(Operation operation) {
        List<ParamInfo> out = new ArrayList<>();

        if (operation.getParameters() != null) {
            for (var parameter : operation.getParameters()) {
                // var typeName = TypeMapper.toTypeName(parameter.getSchema());
                var typeName = "";
                out.add(new ParamInfo(typeName, parameter.getName(), parameter.getIn()));
            }
        }

        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            var json = operation.getRequestBody().getContent().get("application/json");
            if (json != null && json.getSchema() != null) {
                // var javaType = TypeMapper.toJava(json.getSchema());
                var javaType = "";
                out.add(new ParamInfo(javaType, "body", "body"));
            }
        }

        return out;
    }
}
