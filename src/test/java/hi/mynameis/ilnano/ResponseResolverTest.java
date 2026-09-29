package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ResponseResolverTest {

    private final ResponseResolver testSubject = new ResponseResolver();

    private static ApiResponse jsonResponse(String ref) {
        return new ApiResponse().content(new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().$ref(ref))));
    }

    @Test
    void operation_without_responses_produces_nothing() {
        var resolved = testSubject.resolve(new Operation());

        assertThat(resolved.schema()).isNull();
        assertThat(resolved.mediaTypes()).isEmpty();
    }

    @Test
    void response_without_content_produces_nothing() {
        var operation = new Operation().responses(
                new ApiResponses().addApiResponse("204", new ApiResponse().description("gone")));

        var resolved = testSubject.resolve(operation);

        assertThat(resolved.schema()).isNull();
        assertThat(resolved.mediaTypes()).isEmpty();
    }

    @Test
    void picks_the_schema_and_media_type_of_the_success_response() {
        var operation = new Operation().responses(
                new ApiResponses().addApiResponse("200", jsonResponse("#/components/schemas/Pet")));

        var resolved = testSubject.resolve(operation);

        assertThat(resolved.schema().get$ref()).isEqualTo("#/components/schemas/Pet");
        assertThat(resolved.mediaTypes()).containsExactly("application/json");
    }

    @Test
    void prefers_the_lowest_successful_status_over_errors() {
        var operation = new Operation().responses(new ApiResponses()
                .addApiResponse("404", jsonResponse("#/components/schemas/Problem"))
                .addApiResponse("201", jsonResponse("#/components/schemas/Created"))
                .addApiResponse("200", jsonResponse("#/components/schemas/Pet")));

        assertThat(testSubject.resolve(operation).schema().get$ref())
                .isEqualTo("#/components/schemas/Pet");
    }

    @Test
    void falls_back_to_the_default_response_when_no_success_status_exists() {
        var operation = new Operation().responses(new ApiResponses()
                .addApiResponse("500", jsonResponse("#/components/schemas/ServerError"))
                .addApiResponse("default", jsonResponse("#/components/schemas/Fallback")));

        assertThat(testSubject.resolve(operation).schema().get$ref())
                .isEqualTo("#/components/schemas/Fallback");
    }

    @Test
    void reports_every_media_type_the_success_response_declares() {
        var content = new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().type("string")))
                .addMediaType("application/xml", new MediaType().schema(new Schema<>().type("string")));
        var operation = new Operation().responses(
                new ApiResponses().addApiResponse("200", new ApiResponse().content(content)));

        assertThat(testSubject.resolve(operation).mediaTypes())
                .containsExactlyInAnyOrder("application/json", "application/xml");
    }

    @Test
    void error_only_responses_produce_nothing() {
        var operation = new Operation().responses(new ApiResponses()
                .addApiResponse("400", jsonResponse("#/components/schemas/Problem")));

        var resolved = testSubject.resolve(operation);

        assertThat(resolved.schema()).isNull();
        assertThat(resolved.mediaTypes()).isEmpty();
    }
}
