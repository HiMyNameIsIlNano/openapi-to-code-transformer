package hi.mynameis.ilnano.rewrite;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class QuarkusRestClientRecipeTest {

    private final RecipeRunner runner = new RecipeRunner();

    private List<String> transform(String... apis) {
        var sources = new ArrayList<>(GeneratedApiFixture.models());
        sources.addAll(List.of(apis));

        return runner.run(new QuarkusRestClientRecipe(), sources);
    }

    private String transformed(String api, String typeName) {
        return runner.sourceNamed(transform(api), typeName);
    }

    @Test
    void the_interface_becomes_a_rest_client_with_a_config_key_from_its_tag() {
        assertThat(transformed(GeneratedApiFixture.PETS_API, "PetsApi"))
                .contains("@RegisterRestClient(configKey = \"pets-api\")")
                .contains("import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;");
    }

    /**
     * {@code /pets} and {@code /pets/{petId}} share one segment, so the interface carries it and the
     * methods only keep what is left.
     */
    @Test
    void the_shared_path_prefix_is_hoisted_onto_the_interface() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@Path(\"/pets\")")
                .contains("@Path(\"/{petId}\")")
                .doesNotContain("@Path(\"/pets/{petId}\")");
    }

    @Test
    void a_lone_operation_keeps_its_path_on_the_method() {
        var rewritten = transformed(GeneratedApiFixture.OWNERS_API, "OwnersApi");

        assertThat(rewritten)
                .contains("@Path(\"/owners\")")
                .contains("@RegisterRestClient(configKey = \"owners-api\")");
    }

    @Test
    void every_verb_becomes_its_jax_rs_annotation() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten).contains("@GET").contains("@POST").contains("@DELETE");
    }

    @Test
    void each_parameter_location_becomes_its_annotation_and_the_body_stays_bare() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@QueryParam(\"limit\") Integer limit")
                .contains("@HeaderParam(\"X-Trace-Id\") String xTraceId")
                .contains("@PathParam(\"petId\") UUID petId")
                .contains("createPet(Pet body)");
    }

    @Test
    void the_produced_media_types_become_a_produces_annotation() {
        assertThat(transformed(GeneratedApiFixture.PETS_API, "PetsApi"))
                .contains("@Produces(\"application/json\")");
        assertThat(transformed(GeneratedApiFixture.OWNERS_API, "OwnersApi"))
                .contains("@Produces({\"application/json\", \"application/xml\"})");
    }

    @Test
    void an_operation_with_a_body_declares_what_it_consumes() {
        assertThat(transformed(GeneratedApiFixture.PETS_API, "PetsApi"))
                .contains("@Consumes(\"application/json\")");
    }

    /**
     * {@code jakarta.ws.rs} has no {@code @TRACE}, and {@code @HttpMethod} only targets
     * {@code ANNOTATION_TYPE}, so the verb annotation has to be generated.
     */
    @Test
    void a_verb_jax_rs_does_not_declare_gets_a_generated_annotation() {
        var rewritten = transform(GeneratedApiFixture.EXOTIC_API);

        assertThat(runner.sourceNamed(rewritten, "DiagnosticsApi"))
                .contains("@TRACE")
                .contains("@HEAD");

        assertThat(rewritten)
                .anySatisfy(source -> assertThat(source)
                        .contains("public @interface TRACE")
                        .contains("@HttpMethod(\"TRACE\")")
                        .contains("@Target(ElementType.METHOD)"));
    }

    @Test
    void the_markers_are_gone_along_with_their_imports() {
        assertThat(transformed(GeneratedApiFixture.PETS_API, "PetsApi"))
                .doesNotContain("@ApiInterface")
                .doesNotContain("@ApiAnnotation")
                .doesNotContain("@ApiParam")
                .doesNotContain("hi.mynameis.ilnano");
    }

    @Test
    void an_interface_without_the_markers_is_left_alone() {
        assertThat(transformed(GeneratedApiFixture.UNMARKED, "NotGenerated"))
                .isEqualTo(GeneratedApiFixture.UNMARKED);
    }
}
