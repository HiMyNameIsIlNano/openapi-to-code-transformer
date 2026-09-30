package hi.mynameis.ilnano.rewrite;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class SpringHttpServiceClientRecipeTest {

    private final RecipeRunner runner = new RecipeRunner();

    private List<String> transform(String... apis) {
        return transform(new SpringHttpServiceClientRecipe(), apis);
    }

    private List<String> transform(SpringHttpServiceClientRecipe recipe, String... apis) {
        var sources = new ArrayList<>(GeneratedApiFixture.models());
        sources.addAll(List.of(apis));

        return runner.run(recipe, sources);
    }

    private String transformed(String api, String typeName) {
        return runner.sourceNamed(transform(api), typeName);
    }

    @Test
    void the_shared_path_prefix_becomes_the_interface_http_exchange() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@HttpExchange(\"/pets\")")
                .contains("import org.springframework.web.service.annotation.HttpExchange;");
    }

    @Test
    void each_verb_becomes_its_exchange_shortcut() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@GetExchange")
                .contains("@PostExchange")
                .contains("@DeleteExchange");
    }

    /**
     * Java forbids mixing the unnamed shorthand with a named member, so a path that travels together
     * with {@code accept} has to be spelled out as {@code value}.
     */
    @Test
    void a_path_next_to_another_member_is_named_explicitly() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@GetExchange(value = \"/{petId}\", accept = \"application/json\")")
                .contains("@DeleteExchange(\"/{petId}\")");
    }

    @Test
    void the_produced_media_types_become_the_accept_member() {
        assertThat(transformed(GeneratedApiFixture.OWNERS_API, "OwnersApi"))
                .contains("accept = {\"application/json\", \"application/xml\"}");
    }

    /**
     * Spring binds by parameter name, so only a spec name that differs from the Java one needs
     * spelling out.
     */
    @Test
    void a_parameter_is_only_named_when_the_spec_name_differs() {
        var rewritten = transformed(GeneratedApiFixture.PETS_API, "PetsApi");

        assertThat(rewritten)
                .contains("@RequestParam(required = false) Integer limit")
                .contains("@RequestHeader(value = \"X-Trace-Id\", required = false) String xTraceId")
                .contains("@PathVariable UUID petId")
                .contains("@RequestBody Pet body");
    }

    @Test
    void a_verb_without_a_shortcut_uses_the_long_form() {
        assertThat(runner.sourceNamed(transform(GeneratedApiFixture.EXOTIC_API), "DiagnosticsApi"))
                .contains("@HttpExchange(value = \"/trace\", method = \"TRACE\")")
                .contains("@HttpExchange(value = \"/ping\", method = \"HEAD\")");
    }

    @Test
    void a_configuration_class_registers_the_clients() {
        assertThat(transform(GeneratedApiFixture.PETS_API))
                .anySatisfy(source -> assertThat(source)
                        .contains("@ImportHttpServices(group = \"api\", basePackages = \"com.acme.generated.api\")")
                        .contains("@Configuration(proxyBeanMethods = false)")
                        .contains("public class HttpServiceClientConfig"));
    }

    @Test
    void the_configured_group_names_the_client_group() {
        assertThat(transform(new SpringHttpServiceClientRecipe("pet-store"), GeneratedApiFixture.PETS_API))
                .anySatisfy(source -> assertThat(source)
                        .contains("@ImportHttpServices(group = \"pet-store\""));
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
