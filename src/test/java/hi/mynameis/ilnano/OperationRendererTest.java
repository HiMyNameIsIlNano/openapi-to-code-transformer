package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class OperationRendererTest {

    private static final String MODEL_PACKAGE = "com.acme.model";

    private final OperationRenderer testSubject =
            new OperationRenderer(new TypeMapper(MODEL_PACKAGE), new ParameterNamer());

    private static OperationInfo operation(ResolvedResponse response, List<ParamInfo> params) {
        return new OperationInfo(OperationTypeEnum.GET, "/pets", "listPets", response, params);
    }

    @Test
    void renders_an_abstract_annotated_method() {
        var rendered = testSubject.render(operation(ResolvedResponse.empty(), List.of())).toString();

        assertThat(rendered)
                .contains("type = hi.mynameis.ilnano.OperationTypeEnum.GET")
                .contains("path = \"/pets\"")
                .contains("void listPets()");
    }

    @Test
    void omits_produces_when_the_response_declares_no_media_type() {
        var rendered = testSubject.render(operation(ResolvedResponse.empty(), List.of())).toString();

        assertThat(rendered).doesNotContain("produces");
    }

    @Test
    void renders_a_single_produced_media_type_as_a_bare_string() {
        var response = new ResolvedResponse(
                new Schema<>().type("string"), List.of("application/json"));

        assertThat(testSubject.render(operation(response, List.of())).toString())
                .contains("produces = \"application/json\"")
                .doesNotContain("produces = {");
    }

    @Test
    void renders_multiple_produced_media_types_as_an_array() {
        var response = new ResolvedResponse(new Schema<>().type("string"),
                List.of("application/json", "application/xml"));

        assertThat(testSubject.render(operation(response, List.of())).toString())
                .contains("produces = {\"application/json\", \"application/xml\"}");
    }

    @Test
    void annotates_each_parameter_with_its_name_and_location() {
        var params = List.of(
                new ParamInfo("petId", ParameterLocation.PATH, new Schema<>().type("string"), true),
                new ParamInfo("limit", ParameterLocation.QUERY, new Schema<>().type("integer"), false));

        var rendered = testSubject.render(operation(ResolvedResponse.empty(), params)).toString();

        assertThat(rendered)
                .contains("name = \"petId\"")
                .contains("in = hi.mynameis.ilnano.ParameterLocation.PATH")
                .contains("required = true")
                .contains("name = \"limit\"")
                .contains("in = hi.mynameis.ilnano.ParameterLocation.QUERY")
                .contains("required = false");
    }

    @Test
    void uses_the_response_schema_as_the_return_type() {
        var response = new ResolvedResponse(
                new Schema<>().$ref("#/components/schemas/Pet"), List.of("application/json"));

        assertThat(testSubject.render(operation(response, List.of())).toString())
                .contains(MODEL_PACKAGE + ".Pet listPets()");
    }

    @Test
    void sanitises_parameter_names_while_keeping_the_spec_name_in_the_annotation() {
        var params = List.of(
                new ParamInfo("X-Trace-Id", ParameterLocation.HEADER, new Schema<>().type("string"), false));

        var rendered = testSubject.render(operation(ResolvedResponse.empty(), params)).toString();

        assertThat(rendered)
                .contains("name = \"X-Trace-Id\"")
                .contains("String xTraceId");
    }
}
