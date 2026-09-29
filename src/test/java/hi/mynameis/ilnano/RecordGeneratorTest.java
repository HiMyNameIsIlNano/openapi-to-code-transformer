package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class RecordGeneratorTest {

    private static final String MODEL_PACKAGE = "com.acme.model";

    private final RecordGenerator testSubject =
            new RecordGenerator(MODEL_PACKAGE, new TypeMapper(MODEL_PACKAGE), new ParameterNamer());

    @Test
    void renders_a_record_named_after_the_schema() {
        var pet = new Schema<>().type("object")
                .addProperty("name", new Schema<>().type("string"));

        assertThat(testSubject.render("Pet", pet))
                .contains("package " + MODEL_PACKAGE + ";")
                .contains("public record Pet(");
    }

    @Test
    void maps_each_property_to_a_typed_component() {
        var pet = new Schema<>().type("object")
                .addProperty("name", new Schema<>().type("string"))
                .addProperty("age", new Schema<>().type("integer").format("int32"))
                .addProperty("id", new Schema<>().type("string").format("uuid"));

        assertThat(testSubject.render("Pet", pet))
                .contains("String name")
                .contains("Integer age")
                .contains("UUID id");
    }

    @Test
    void maps_array_properties_to_lists() {
        var owner = new Schema<>().type("object")
                .addProperty("pets", new Schema<>().type("array")
                        .items(new Schema<>().$ref("#/components/schemas/Pet")));

        assertThat(testSubject.render("Owner", owner)).contains("List<Pet> pets");
    }

    @Test
    void maps_referenced_properties_to_model_types() {
        var owner = new Schema<>().type("object")
                .addProperty("favourite", new Schema<>().$ref("#/components/schemas/Pet"));

        assertThat(testSubject.render("Owner", owner)).contains("Pet favourite");
    }

    @Test
    void renders_a_marker_record_when_the_schema_has_no_properties() {
        assertThat(testSubject.render("Empty", new Schema<>().type("object")))
                .contains("public record Empty()");
    }

    @Test
    void sanitises_property_names_that_are_not_valid_identifiers() {
        var schema = new Schema<>().type("object")
                .addProperty("user-name", new Schema<>().type("string"))
                .addProperty("class", new Schema<>().type("string"));

        assertThat(testSubject.render("Odd", schema))
                .contains("String userName")
                .contains("String classParam");
    }
}
