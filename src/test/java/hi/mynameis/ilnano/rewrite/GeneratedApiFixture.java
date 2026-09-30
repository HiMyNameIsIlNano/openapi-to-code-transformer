package hi.mynameis.ilnano.rewrite;

import java.util.List;

/**
 * Generated sources to run the recipes against, written in the exact shape
 * {@code InterfacePojoGenerator} and {@code OperationRenderer} emit.
 *
 * <p>Held as text rather than produced by calling the generator so that a recipe test states its own
 * input, and so that shapes the current specs do not exercise — a {@code TRACE} operation, a
 * cookie parameter — can be covered too.
 */
final class GeneratedApiFixture {

    private GeneratedApiFixture() {
    }

    /**
     * A tag with two paths sharing a prefix, covering path, query, header and body parameters as
     * well as an operation with no response body.
     */
    static final String PETS_API = """
            package com.acme.generated.api;

            import com.acme.generated.model.Pet;
            import hi.mynameis.ilnano.ApiAnnotation;
            import hi.mynameis.ilnano.ApiInterface;
            import hi.mynameis.ilnano.ApiParam;
            import hi.mynameis.ilnano.OperationTypeEnum;
            import hi.mynameis.ilnano.ParameterLocation;
            import java.util.List;
            import java.util.UUID;

            @ApiInterface(
                    tag = "Pets"
            )
            public interface PetsApi {
                @ApiAnnotation(
                        type = OperationTypeEnum.GET,
                        path = "/pets",
                        produces = "application/json"
                )
                List<Pet> listPets(@ApiParam(name = "limit", in = ParameterLocation.QUERY, required = false) Integer limit,
                        @ApiParam(name = "X-Trace-Id", in = ParameterLocation.HEADER, required = false) String xTraceId);

                @ApiAnnotation(
                        type = OperationTypeEnum.POST,
                        path = "/pets",
                        produces = "application/json"
                )
                Pet createPet(@ApiParam(name = "body", in = ParameterLocation.BODY, required = true) Pet body);

                @ApiAnnotation(
                        type = OperationTypeEnum.GET,
                        path = "/pets/{petId}",
                        produces = "application/json"
                )
                Pet getPet(@ApiParam(name = "petId", in = ParameterLocation.PATH, required = true) UUID petId);

                @ApiAnnotation(
                        type = OperationTypeEnum.DELETE,
                        path = "/pets/{petId}"
                )
                void deletePet(@ApiParam(name = "petId", in = ParameterLocation.PATH, required = true) UUID petId);
            }
            """;

    /**
     * A tag with a single operation, which must keep its path on the method, and two produced media
     * types.
     */
    static final String OWNERS_API = """
            package com.acme.generated.api;

            import com.acme.generated.model.Owner;
            import hi.mynameis.ilnano.ApiAnnotation;
            import hi.mynameis.ilnano.ApiInterface;
            import hi.mynameis.ilnano.OperationTypeEnum;
            import java.util.List;

            @ApiInterface(
                    tag = "Owners"
            )
            public interface OwnersApi {
                @ApiAnnotation(
                        type = OperationTypeEnum.GET,
                        path = "/owners",
                        produces = {"application/json", "application/xml"}
                )
                List<Owner> listOwners();
            }
            """;

    /**
     * The verbs and parameter locations without a dedicated annotation in either framework.
     */
    static final String EXOTIC_API = """
            package com.acme.generated.api;

            import hi.mynameis.ilnano.ApiAnnotation;
            import hi.mynameis.ilnano.ApiInterface;
            import hi.mynameis.ilnano.ApiParam;
            import hi.mynameis.ilnano.OperationTypeEnum;
            import hi.mynameis.ilnano.ParameterLocation;

            @ApiInterface(
                    tag = "Diagnostics"
            )
            public interface DiagnosticsApi {
                @ApiAnnotation(
                        type = OperationTypeEnum.TRACE,
                        path = "/diagnostics/trace"
                )
                void traceIt(@ApiParam(name = "session", in = ParameterLocation.COOKIE, required = false) String session);

                @ApiAnnotation(
                        type = OperationTypeEnum.HEAD,
                        path = "/diagnostics/ping"
                )
                void ping();
            }
            """;

    /**
     * A hand-written interface with no markers, which every recipe must leave untouched.
     */
    static final String UNMARKED = """
            package com.acme.app;

            public interface NotGenerated {
                String hello();
            }
            """;

    static final String PET_MODEL = """
            package com.acme.generated.model;

            import java.util.UUID;

            public record Pet(UUID id, String name) {
            }
            """;

    static final String OWNER_MODEL = """
            package com.acme.generated.model;

            import java.util.UUID;

            public record Owner(UUID id, String fullName) {
            }
            """;

    /**
     * The models the API interfaces refer to, which have to be parsed alongside them.
     */
    static List<String> models() {
        return List.of(PET_MODEL, OWNER_MODEL);
    }
}
