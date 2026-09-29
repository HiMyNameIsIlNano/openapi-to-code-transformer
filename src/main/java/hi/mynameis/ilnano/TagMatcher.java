package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TagMatcher {

    private final Mapper mapper;

    private final ResponseResolver responseResolver;

    private final ParameterResolver parameterResolver;

    TagMatcher() {
        this(new TagMapper(TagMatcher::capitalize), new ResponseResolver(), new ParameterResolver());
    }

    TagMatcher(Mapper mapper, ResponseResolver responseResolver, ParameterResolver parameterResolver) {
        this.mapper = mapper;
        this.responseResolver = responseResolver;
        this.parameterResolver = parameterResolver;
    }

    public Map<ApiTag, List<OperationInfo>> groupByTag(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");

        return Optional.ofNullable(openAPI.getPaths())
                .map(Map::entrySet)
                .orElseGet(Set::of)
                .stream()
                .flatMap(this::toTaggedOperations)
                .collect(Collectors.groupingBy(Entry::getKey,
                        Collectors.mapping(Entry::getValue, Collectors.toUnmodifiableList())));
    }

    private Stream<Entry<ApiTag, OperationInfo>> toTaggedOperations(Entry<String, PathItem> pathEntry) {
        return pathEntry.getValue().readOperationsMap().entrySet().stream()
                .map(methodEntry -> {
                    var operation = methodEntry.getValue();
                    return Map.entry(
                            mapper.mapTags(operation.getTags()),
                            new OperationInfo(
                                    OperationTypeEnum.from(methodEntry.getKey()),
                                    pathEntry.getKey(),
                                    operation.getOperationId(),
                                    responseResolver.resolve(operation),
                                    parameterResolver.resolve(operation)));
                });
    }

    private static String capitalize(String tag) {
        return tag.isEmpty() ? tag : Character.toUpperCase(tag.charAt(0)) + tag.substring(1);
    }
}
