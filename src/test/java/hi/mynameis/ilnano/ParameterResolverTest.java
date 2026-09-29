package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

final class ParameterResolverTest {

    private final ParameterResolver testSubject = new ParameterResolver();

    private static Parameter parameter(String name, String in, boolean required) {
        return new Parameter().name(name).in(in).required(required)
                .schema(new Schema<>().type("string"));
    }

    @Test
    void operation_without_parameters_or_body_resolves_to_nothing() {
        assertThat(testSubject.resolve(new Operation())).isEmpty();
    }

    @Test
    void maps_each_parameter_to_its_location() {
        var operation = new Operation()
                .addParametersItem(parameter("id", "path", true))
                .addParametersItem(parameter("limit", "query", false))
                .addParametersItem(parameter("X-Trace", "header", false))
                .addParametersItem(parameter("session", "cookie", false));

        assertThat(testSubject.resolve(operation))
                .extracting(ParamInfo::name, ParamInfo::in)
                .containsExactly(
                        tuple("id", ParameterLocation.PATH),
                        tuple("limit", ParameterLocation.QUERY),
                        tuple("X-Trace", ParameterLocation.HEADER),
                        tuple("session", ParameterLocation.COOKIE));
    }

    @Test
    void preserves_whether_a_parameter_is_required() {
        var operation = new Operation()
                .addParametersItem(parameter("id", "path", true))
                .addParametersItem(parameter("limit", "query", false));

        assertThat(testSubject.resolve(operation))
                .extracting(ParamInfo::required)
                .containsExactly(true, false);
    }

    @Test
    void request_body_becomes_a_trailing_body_parameter() {
        var operation = new Operation().requestBody(new RequestBody()
                .required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/Pet")))));

        assertThat(testSubject.resolve(operation))
                .singleElement()
                .satisfies(param -> {
                    assertThat(param.in()).isEqualTo(ParameterLocation.BODY);
                    assertThat(param.required()).isTrue();
                    assertThat(param.schema().get$ref()).isEqualTo("#/components/schemas/Pet");
                });
    }

    @Test
    void body_parameter_comes_after_the_declared_parameters() {
        var operation = new Operation()
                .addParametersItem(parameter("id", "path", true))
                .requestBody(new RequestBody().content(new Content().addMediaType(
                        "application/json", new MediaType().schema(new Schema<>().type("string")))));

        assertThat(testSubject.resolve(operation))
                .extracting(ParamInfo::in)
                .containsExactly(ParameterLocation.PATH, ParameterLocation.BODY);
    }

    @Test
    void request_body_without_content_is_ignored() {
        var operation = new Operation().requestBody(new RequestBody().required(true));

        assertThat(testSubject.resolve(operation)).isEmpty();
    }

    @Test
    void parameter_without_explicit_location_defaults_to_query() {
        var operation = new Operation().addParametersItem(
                new Parameter().name("stray").schema(new Schema<>().type("string")));

        assertThat(testSubject.resolve(operation))
                .singleElement()
                .extracting(ParamInfo::in)
                .isEqualTo(ParameterLocation.QUERY);
    }
}
