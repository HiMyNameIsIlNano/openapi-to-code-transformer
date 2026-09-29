package hi.mynameis.ilnano;

import io.swagger.v3.core.util.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;

final class OpenApiSpecHelper {

     OpenAPI deepCopy(OpenAPI spec) {
        ObjectMapper mapper = Json.mapper();

        try {
            return mapper.readValue(mapper.writeValueAsString(spec), OpenAPI.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deep-copy OpenAPI spec", e);
        }
    }

}
