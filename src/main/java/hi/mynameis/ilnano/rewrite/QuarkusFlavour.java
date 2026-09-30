package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;
import hi.mynameis.ilnano.ParameterLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code jakarta.ws.rs} flavour, registered as a MicroProfile REST client.
 *
 * <p>Produces the shape a Quarkus project expects:
 *
 * <pre>{@code
 * @RegisterRestClient(configKey = "pets-api")
 * @Path("/pets")
 * public interface PetsApi {
 *     @GET
 *     @Produces("application/json")
 *     List<Pet> listPets(@QueryParam("limit") Integer limit);
 * }
 * }</pre>
 *
 * The base URL then comes from {@code quarkus.rest-client.pets-api.url}.
 */
final class QuarkusFlavour implements ClientFlavour {

    private static final String WS_RS = "jakarta.ws.rs";
    private static final String REGISTER_REST_CLIENT =
            "org.eclipse.microprofile.rest.client.inject.RegisterRestClient";

    /**
     * {@code jakarta.ws.rs} has a dedicated annotation per verb, but only for the verbs it knows.
     * A verb that is missing here needs a custom annotation, which
     * {@link CustomVerbAnnotation} generates into the interface's own package.
     */
    private static final Map<OperationTypeEnum, String> VERB_ANNOTATIONS = Map.of(
            OperationTypeEnum.GET, "GET",
            OperationTypeEnum.POST, "POST",
            OperationTypeEnum.PUT, "PUT",
            OperationTypeEnum.PATCH, "PATCH",
            OperationTypeEnum.DELETE, "DELETE",
            OperationTypeEnum.HEAD, "HEAD",
            OperationTypeEnum.OPTIONS, "OPTIONS");

    private static final Map<ParameterLocation, String> PARAM_ANNOTATIONS = Map.of(
            ParameterLocation.PATH, "PathParam",
            ParameterLocation.QUERY, "QueryParam",
            ParameterLocation.HEADER, "HeaderParam",
            ParameterLocation.COOKIE, "CookieParam");

    @Override
    public List<String> interfaceAnnotations(String tag, ResourcePath commonPath) {
        var annotations = new ArrayList<String>();

        annotations.add("@RegisterRestClient(configKey = \"%s\")".formatted(configKey(tag)));
        if (!commonPath.isEmpty()) {
            annotations.add("@Path(\"%s\")".formatted(commonPath));
        }
        return annotations;
    }

    /**
     * A config key in the kebab-case Quarkus configuration uses, so {@code Pets} becomes
     * {@code pets-api} and {@code PetOwners} becomes {@code pet-owners-api}.
     */
    private String configKey(String tag) {
        var kebab = tag.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "-")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .toLowerCase(Locale.ROOT);

        return kebab.isEmpty() ? "api" : kebab + "-api";
    }

    @Override
    public List<String> methodAnnotations(OperationDescriptor operation, ResourcePath path) {
        var annotations = new ArrayList<String>();

        annotations.add(verbAnnotation(operation.method()));
        if (!path.isEmpty()) {
            annotations.add("@Path(\"%s\")".formatted(path));
        }
        if (!operation.mediaTypes().isEmpty()) {
            annotations.add("@Produces(%s)".formatted(mediaTypes(operation.mediaTypes())));
        }
        if (hasBody(operation)) {
            annotations.add("@Consumes(\"application/json\")");
        }
        return annotations;
    }

    /**
     * {@code @GET} for the verbs JAX-RS declares, and the name of the generated custom annotation
     * for the rest. {@code jakarta.ws.rs.HttpMethod} cannot be used directly, since it only targets
     * {@code ANNOTATION_TYPE}.
     */
    private String verbAnnotation(OperationTypeEnum method) {
        return "@" + VERB_ANNOTATIONS.getOrDefault(method, CustomVerbAnnotation.nameOf(method));
    }

    /**
     * The verbs used by {@code operations} that JAX-RS has no annotation for, and which therefore
     * need one generated.
     */
    static List<OperationTypeEnum> customVerbsOf(List<OperationDescriptor> operations) {
        return operations.stream()
                .map(OperationDescriptor::method)
                .filter(method -> !VERB_ANNOTATIONS.containsKey(method))
                .distinct()
                .toList();
    }

    private String mediaTypes(List<String> mediaTypes) {
        var quoted = mediaTypes.stream().map("\"%s\""::formatted).toList();

        return quoted.size() == 1 ? quoted.get(0) : "{" + String.join(", ", quoted) + "}";
    }

    private boolean hasBody(OperationDescriptor operation) {
        return operation.parameters().stream().anyMatch(ParameterDescriptor::isBody);
    }

    /**
     * The request body carries no annotation in JAX-RS: the single unannotated parameter is the
     * entity.
     */
    @Override
    public List<String> parameterAnnotations(ParameterDescriptor parameter) {
        var annotation = PARAM_ANNOTATIONS.get(parameter.in());

        return annotation == null
                ? List.of()
                : List.of("@%s(\"%s\")".formatted(annotation, parameter.specName()));
    }

    @Override
    public List<String> imports() {
        return List.of(
                REGISTER_REST_CLIENT,
                WS_RS + ".Consumes",
                WS_RS + ".CookieParam",
                WS_RS + ".DELETE",
                WS_RS + ".GET",
                WS_RS + ".HEAD",
                WS_RS + ".HeaderParam",
                WS_RS + ".HttpMethod",
                WS_RS + ".OPTIONS",
                WS_RS + ".PATCH",
                WS_RS + ".POST",
                WS_RS + ".PUT",
                WS_RS + ".Path",
                WS_RS + ".PathParam",
                WS_RS + ".Produces",
                WS_RS + ".QueryParam");
    }
}
