package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class InlineSchemaFlatteningTest implements OpenApiLoader {

    private Map<String, Schema> schemas(OpenAPI api) {
        return api.getComponents().getSchemas();
    }

    @Test
    void nested_inline_object_is_hoisted_into_a_named_top_level_schema() {
        var flattened = load("src/test/resources/hoisting/nested-inline-object.yaml");

        assertThat(schemas(flattened)).containsKey("PetAddress");
        assertThat(schemas(flattened).get("PetAddress").getProperties())
                .containsKeys("street", "city");
    }

    @Test
    void hoisted_property_is_replaced_by_a_reference() {
        var flattened = load("src/test/resources/hoisting/nested-inline-object.yaml");

        var address = (Schema<?>) schemas(flattened).get("Pet").getProperties().get("address");

        assertThat(address.get$ref()).isEqualTo("#/components/schemas/PetAddress");
        assertThat(address.getProperties()).isNull();
    }

    @Test
    void inline_object_inside_an_array_is_hoisted_and_the_array_is_preserved() {
        var flattened = load("src/test/resources/hoisting/array-of-inline-object.yaml");

        var pets = (Schema<?>) schemas(flattened).get("Owner").getProperties().get("pets");

        assertThat(pets.getType()).isEqualTo("array");
        assertThat(pets.getItems().get$ref()).startsWith("#/components/schemas/");
    }

    @Test
    void already_flat_spec_keeps_its_schemas_and_references() {
        var flattened = load("src/test/resources/hoisting/already-flat.yaml");

        assertThat(schemas(flattened)).containsOnlyKeys("Pet", "Address");
        assertThat(((Schema<?>) schemas(flattened).get("Pet").getProperties().get("address")).get$ref())
                .isEqualTo("#/components/schemas/Address");
    }

    @Test
    void spec_without_components_is_handled_gracefully() {
        var flattened = load("src/test/resources/hoisting/no-components.yaml");

        assertThat(flattened.getComponents() == null
                || flattened.getComponents().getSchemas() == null
                || flattened.getComponents().getSchemas().isEmpty()).isTrue();
    }

    @Test
    void flattened_models_are_generated_as_records() {
        var generated = new JavaGenerator()
                .generate(load("src/test/resources/hoisting/nested-inline-object.yaml"), "com.acme");

        assertThat(generated.models())
                .anySatisfy(model -> assertThat(model).contains("record PetAddress("))
                .anySatisfy(model -> assertThat(model).contains("PetAddress address"));
    }
}
