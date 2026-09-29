package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

final class TypeMapperTest {

    private static final String MODEL_PACKAGE = "com.acme.model";

    private final TypeMapper testSubject = new TypeMapper(MODEL_PACKAGE);

    private static Schema<?> schema(String type, String format) {
        return new Schema<>().type(type).format(format);
    }

    @ParameterizedTest
    @CsvSource({
            "integer,          , java.lang.Integer",
            "integer, int32    , java.lang.Integer",
            "integer, int64    , java.lang.Long",
            "number,           , java.lang.Double",
            "number,  double   , java.lang.Double",
            "number,  float    , java.lang.Float",
            "boolean,          , java.lang.Boolean",
            "string,           , java.lang.String",
            "string,  date     , java.time.LocalDate",
            "string,  date-time, java.time.OffsetDateTime",
            "string,  uuid     , java.util.UUID",
            "string,  binary   , byte[]",
            "object,           , java.lang.Object"
    })
    void maps_primitive_types_and_formats(String type, String format, String expected) {
        assertThat(testSubject.toTypeName(schema(type, format)))
                .hasToString(expected);
    }

    @Test
    void unknown_format_falls_back_to_the_types_default() {
        assertThat(testSubject.toTypeName(schema("string", "e-mail")))
                .hasToString("java.lang.String");
    }

    @Test
    void absent_schema_maps_to_object() {
        assertThat(testSubject.toTypeName(null)).hasToString("java.lang.Object");
    }

    @Test
    void untyped_schema_maps_to_object() {
        assertThat(testSubject.toTypeName(new Schema<>())).hasToString("java.lang.Object");
    }

    @Test
    void reference_maps_to_a_class_in_the_model_package() {
        var ref = new Schema<>().$ref("#/components/schemas/Pet");

        assertThat(testSubject.toTypeName(ref)).hasToString(MODEL_PACKAGE + ".Pet");
    }

    @Test
    void array_of_primitives_maps_to_a_parameterized_list() {
        var array = new Schema<>().type("array").items(schema("string", null));

        assertThat(testSubject.toTypeName(array))
                .hasToString("java.util.List<java.lang.String>");
    }

    @Test
    void array_of_references_maps_to_a_list_of_model_classes() {
        var array = new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/Pet"));

        assertThat(testSubject.toTypeName(array))
                .hasToString("java.util.List<" + MODEL_PACKAGE + ".Pet>");
    }

    @Test
    void nested_arrays_nest_the_lists() {
        var inner = new Schema<>().type("array").items(schema("integer", "int64"));
        var outer = new Schema<>().type("array").items(inner);

        assertThat(testSubject.toTypeName(outer))
                .hasToString("java.util.List<java.util.List<java.lang.Long>>");
    }

    @Test
    void array_without_items_maps_to_a_list_of_object() {
        assertThat(testSubject.toTypeName(new Schema<>().type("array")))
                .hasToString("java.util.List<java.lang.Object>");
    }
}
