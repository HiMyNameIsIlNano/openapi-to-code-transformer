package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class InlineObjectHoisterTest implements OpenApiLoader {

    private final InlineObjectHoister hoister = new InlineObjectHoister();

    private Map<String, Schema> schemas(OpenAPI api) {
        return api.getComponents().getSchemas();
    }

    @Test
    void hoistsNestedInlineObjectIntoNamedTopLevelSchema() {
        OpenAPI original = load("src/test/resources/hoisting/nested-inline-object.yaml");

        OpenAPI result = hoister.hoist(original);

        // A new schema for the hoisted address should now exist.
        assertThat(schemas(result)).containsKey("PetAddress");

        // The hoisted schema carries the inline object's own properties.
        Schema<?> petAddress = schemas(result).get("PetAddress");
        assertThat(petAddress.getProperties()).containsKeys("street", "city");
    }

    @Test
    void rewiresParentPropertyToRef() {
        OpenAPI result = hoister.hoist(load("src/test/resources/hoisting/nested-inline-object.yaml"));

        Schema<?> pet = schemas(result).get("Pet");
        Schema<?> addressProp = (Schema<?>) pet.getProperties().get("address");

        // No longer an inline object — now a reference.
        assertThat(addressProp.get$ref()).isEqualTo("#/components/schemas/PetAddress");
        assertThat(addressProp.getProperties()).isNull();
        assertThat(addressProp.getType()).isNull();
    }

    @Test
    void hoistsInlineObjectNestedInsideAnArray() {
        OpenAPI result = hoister.hoist(load("src/test/resources/hoisting/array-of-inline-object.yaml"));

        // The array item was hoisted...
        assertThat(schemas(result)).containsKey("OwnerPets");

        // ...and the array's items now point to the ref, preserving the array wrapper.
        Schema<?> owner = schemas(result).get("Owner");
        Schema<?> petsProp = (Schema<?>) owner.getProperties().get("pets");
        assertThat(petsProp.getType()).isEqualTo("array");
        assertThat(petsProp.getItems().get$ref())
                .isEqualTo("#/components/schemas/OwnerPets");
    }

    // ---------- (3) PURITY: the original input is never mutated ----------

    @Test
    void doesNotMutateTheOriginalSpec() {
        OpenAPI original = load("src/test/resources/hoisting/nested-inline-object.yaml");

        // Snapshot the relevant state BEFORE hoisting.
        Schema<?> originalPet = schemas(original).get("Pet");
        Schema<?> originalAddress = (Schema<?>) originalPet.getProperties().get("address");
        int schemaCountBefore = schemas(original).size();

        hoister.hoist(original); // discard result — we test the input

        // Original schema map must be untouched: still just "Pet", no "PetAddress".
        assertThat(schemas(original)).hasSize(schemaCountBefore);
        assertThat(schemas(original)).doesNotContainKey("PetAddress");

        // The original address property is STILL an inline object, not a $ref.
        assertThat(originalAddress.get$ref()).isNull();
        assertThat(originalAddress.getProperties()).containsKeys("street", "city");
    }

    // ---------- No-op / edge cases ----------

    @Test
    void alreadyFlatSpecIsUnchangedInContent() {
        OpenAPI original = load("src/test/resources/hoisting/already-flat.yaml");
        int before = schemas(original).size();

        OpenAPI result = hoister.hoist(original);

        // No new schemas minted — nothing to hoist.
        assertThat(schemas(result)).hasSize(before);
        assertThat(schemas(result)).containsOnlyKeys("Pet", "Address");

        // The existing $ref is preserved exactly.
        Schema<?> pet = schemas(result).get("Pet");
        Schema<?> addressProp = (Schema<?>) pet.getProperties().get("address");
        assertThat(addressProp.get$ref()).isEqualTo("#/components/schemas/Address");
    }

    @Test
    void specWithoutComponentsIsHandledGracefully() {
        OpenAPI original = load("src/test/resources/hoisting/no-components.yaml");

        // Must not throw (no components/schemas to iterate).
        OpenAPI result = hoister.hoist(original);

        assertThat(result).isNotNull();
        // Either null components or empty schemas — both acceptable, just no NPE.
        assertThat(result.getComponents() == null
                || result.getComponents().getSchemas() == null
                || result.getComponents().getSchemas().isEmpty()).isTrue();
    }

    // ---------- Idempotency ----------

    @Test
    void hoistingIsIdempotent() {
        OpenAPI once  = hoister.hoist(load("src/test/resources/hoisting/nested-inline-object.yaml"));
        OpenAPI twice = hoister.hoist(once);

        // Second pass finds only $refs → mints nothing new.
        assertThat(schemas(twice).keySet()).isEqualTo(schemas(once).keySet());

        Schema<?> petOnce  = schemas(once).get("Pet");
        Schema<?> petTwice = schemas(twice).get("Pet");
        assertThat(((Schema<?>) petTwice.getProperties().get("address")).get$ref())
                .isEqualTo(((Schema<?>) petOnce.getProperties().get("address")).get$ref());
    }
}