package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

final class GeneratedApiAnnotationsTest implements OpenApiLoader {

    private static final String YAML_SPEC = "src/test/resources/generation/annotated-api.yaml";
    private static final String JSON_SPEC = "src/test/resources/generation/annotated-api.json";
    private static final String BASE_PACKAGE = "com.acme.generated";

    private final JavaGenerator testSubject = new JavaGenerator();

    private GeneratedSources generate(String spec) {
        return testSubject.generate(load(spec), BASE_PACKAGE);
    }

    private String apiNamed(String spec, String simpleName) {
        return generate(spec).apis().stream()
                .filter(source -> source.contains("interface " + simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no generated interface named " + simpleName));
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void generates_one_annotated_interface_per_tag(String spec) {
        assertThat(generate(spec).apis())
                .hasSize(2)
                .anySatisfy(source -> assertThat(source).contains("interface PetsApi"))
                .anySatisfy(source -> assertThat(source).contains("interface OwnersApi"));
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void interface_carries_its_tag_annotation(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .containsPattern("@ApiInterface\\(\\s*tag = \"Pets\"\\s*\\)");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void every_operation_carries_a_typed_method_annotation(String spec) {
        var petsApi = apiNamed(spec, "PetsApi");

        assertThat(petsApi)
                .contains("type = OperationTypeEnum.GET")
                .contains("type = OperationTypeEnum.POST")
                .contains("type = OperationTypeEnum.DELETE")
                .contains("path = \"/pets\"")
                .contains("path = \"/pets/{petId}\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void method_annotation_declares_the_produced_media_type(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("produces = \"application/json\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void every_operation_becomes_a_method_named_after_its_operation_id(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("listPets(")
                .contains("createPet(")
                .contains("getPet(")
                .contains("deletePet(");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void parameters_are_annotated_with_their_location(String spec) {
        var petsApi = apiNamed(spec, "PetsApi");

        assertThat(petsApi)
                .contains("@ApiParam(name = \"petId\", in = ParameterLocation.PATH")
                .contains("@ApiParam(name = \"limit\", in = ParameterLocation.QUERY")
                .contains("@ApiParam(name = \"X-Trace-Id\", in = ParameterLocation.HEADER");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void request_body_is_annotated_as_a_body_parameter(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("in = ParameterLocation.BODY");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void referenced_response_schemas_become_model_return_types(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("Pet getPet(")
                .contains("List<Pet> listPets(");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void operations_without_response_content_return_void(String spec) {
        assertThat(apiNamed(spec, "PetsApi")).contains("void deletePet(");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void parameter_types_come_from_the_schema(String spec) {
        var petsApi = apiNamed(spec, "PetsApi");

        assertThat(petsApi).contains("UUID petId");
        assertThat(petsApi).contains("Integer limit");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void generated_interface_declares_the_api_package(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("package " + BASE_PACKAGE + ".api;");
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void annotations_are_imported_so_the_recipe_can_resolve_them(String spec) {
        assertThat(apiNamed(spec, "PetsApi"))
                .contains("import " + ApiAnnotation.class.getCanonicalName() + ";")
                .contains("import " + ApiParam.class.getCanonicalName() + ";")
                .contains("import " + ApiInterface.class.getCanonicalName() + ";");
    }

    @Test
    void yaml_and_json_specs_generate_identical_sources() {
        var fromYaml = generate(YAML_SPEC);
        var fromJson = generate(JSON_SPEC);

        assertThat(fromJson.apis()).containsExactlyInAnyOrderElementsOf(fromYaml.apis());
        assertThat(fromJson.models()).containsExactlyInAnyOrderElementsOf(fromYaml.models());
    }

    @ParameterizedTest
    @ValueSource(strings = {YAML_SPEC, JSON_SPEC})
    void generated_sources_are_valid_compilable_java(String spec) {
        var generated = generate(spec);

        assertThat(new JavaSourceCompiler().compile(generated.all())).isEmpty();
    }
}
