package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;
import hi.mynameis.ilnano.ParameterLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Spring Boot 4 HTTP service client flavour, built on {@code @HttpExchange}.
 *
 * <p>Produces the shape the framework's client registry expects:
 *
 * <pre>{@code
 * @HttpExchange("/pets")
 * public interface PetsApi {
 *     @GetExchange(accept = "application/json")
 *     List<Pet> listPets(@RequestParam(required = false) Integer limit);
 * }
 * }</pre>
 *
 * The interfaces are turned into beans by the {@code @ImportHttpServices} configuration class that
 * {@link SpringHttpServiceClientRecipe} generates, and the base URL comes from
 * {@code spring.http.client.service.group.<group>.base-url}.
 *
 * <p>Note that {@code @RequestMapping} is deliberately not used: the registry only recognises
 * {@code @HttpExchange} and its per-verb variants.
 */
final class SpringFlavour implements ClientFlavour {

    private static final String SERVICE_ANNOTATION = "org.springframework.web.service.annotation";
    private static final String BIND_ANNOTATION = "org.springframework.web.bind.annotation";

    /**
     * The verbs with a dedicated {@code *Exchange} shortcut. The rest fall back to
     * {@code @HttpExchange(method = "...")}.
     */
    private static final Map<OperationTypeEnum, String> EXCHANGE_ANNOTATIONS = Map.of(
            OperationTypeEnum.GET, "GetExchange",
            OperationTypeEnum.POST, "PostExchange",
            OperationTypeEnum.PUT, "PutExchange",
            OperationTypeEnum.PATCH, "PatchExchange",
            OperationTypeEnum.DELETE, "DeleteExchange");

    private static final Map<ParameterLocation, String> PARAM_ANNOTATIONS = Map.of(
            ParameterLocation.PATH, "PathVariable",
            ParameterLocation.QUERY, "RequestParam",
            ParameterLocation.HEADER, "RequestHeader",
            ParameterLocation.COOKIE, "CookieValue",
            ParameterLocation.BODY, "RequestBody");

    @Override
    public List<String> interfaceAnnotations(String tag, ResourcePath commonPath) {
        return commonPath.isEmpty()
                ? List.of("@HttpExchange")
                : List.of("@HttpExchange(\"%s\")".formatted(commonPath));
    }

    @Override
    public List<String> methodAnnotations(OperationDescriptor operation, ResourcePath path) {
        var shortcut = EXCHANGE_ANNOTATIONS.get(operation.method());

        return shortcut == null
                ? List.of(httpExchange(operation, path))
                : List.of(shortcut(shortcut, operation, path));
    }

    /**
     * {@code @GetExchange("/{petId}")}, or {@code @GetExchange(value = "/{petId}", accept = ...)}
     * once there is more than one member — Java only allows the unnamed shorthand on its own.
     */
    private String shortcut(String annotation, OperationDescriptor operation, ResourcePath path) {
        var accept = accept(operation);

        if (path.isEmpty()) {
            return accept.isEmpty()
                    ? "@" + annotation
                    : "@%s(%s)".formatted(annotation, accept.get());
        }
        return accept.isEmpty()
                ? "@%s(\"%s\")".formatted(annotation, path)
                : "@%s(value = \"%s\", %s)".formatted(annotation, path, accept.get());
    }

    /**
     * The long form for the verbs without a shortcut, where {@code method} is mandatory and the
     * path can therefore no longer be the unnamed value.
     */
    private String httpExchange(OperationDescriptor operation, ResourcePath path) {
        var members = new ArrayList<String>();

        if (!path.isEmpty()) {
            members.add("value = \"%s\"".formatted(path));
        }
        members.add("method = \"%s\"".formatted(operation.method().name()));
        accept(operation).ifPresent(members::add);

        return "@HttpExchange(%s)".formatted(String.join(", ", members));
    }

    private java.util.Optional<String> accept(OperationDescriptor operation) {
        var mediaTypes = operation.mediaTypes();

        if (mediaTypes.isEmpty()) {
            return java.util.Optional.empty();
        }

        var quoted = mediaTypes.stream().map("\"%s\""::formatted).toList();

        return java.util.Optional.of("accept = " + (quoted.size() == 1
                ? quoted.get(0)
                : "{" + String.join(", ", quoted) + "}"));
    }

    /**
     * Spring binds by parameter name, so the name is only spelled out when it differs from the Java
     * one — which is what happens to a spec name like {@code X-Trace-Id}. Optional query parameters
     * additionally need {@code required = false}, since the default is {@code true}.
     */
    @Override
    public List<String> parameterAnnotations(ParameterDescriptor parameter) {
        var annotation = PARAM_ANNOTATIONS.get(parameter.in());

        if (annotation == null) {
            return List.of();
        }

        var renamed = !parameter.specName().equals(parameter.javaName());
        var optional = !parameter.required() && supportsRequired(parameter.in());

        // The unnamed shorthand cannot be combined with a named member, so as soon as both are
        // needed the name has to be spelled out as `value`.
        var members = new ArrayList<String>();
        if (renamed) {
            members.add(optional
                    ? "value = \"%s\"".formatted(parameter.specName())
                    : "\"%s\"".formatted(parameter.specName()));
        }
        if (optional) {
            members.add("required = false");
        }

        return List.of(members.isEmpty()
                ? "@" + annotation
                : "@%s(%s)".formatted(annotation, String.join(", ", members)));
    }

    /**
     * A path variable is required by definition; the other locations can be optional.
     */
    private boolean supportsRequired(ParameterLocation in) {
        return in != ParameterLocation.PATH;
    }

    @Override
    public List<String> imports() {
        return List.of(
                SERVICE_ANNOTATION + ".DeleteExchange",
                SERVICE_ANNOTATION + ".GetExchange",
                SERVICE_ANNOTATION + ".HttpExchange",
                SERVICE_ANNOTATION + ".PatchExchange",
                SERVICE_ANNOTATION + ".PostExchange",
                SERVICE_ANNOTATION + ".PutExchange",
                BIND_ANNOTATION + ".CookieValue",
                BIND_ANNOTATION + ".PathVariable",
                BIND_ANNOTATION + ".RequestBody",
                BIND_ANNOTATION + ".RequestHeader",
                BIND_ANNOTATION + ".RequestParam");
    }
}
