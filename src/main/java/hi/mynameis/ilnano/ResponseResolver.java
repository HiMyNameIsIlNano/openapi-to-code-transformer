package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class ResponseResolver {

    private static final String DEFAULT_STATUS = "default";

    ResolvedResponse resolve(Operation operation) {
        return Optional.ofNullable(operation.getResponses())
                .flatMap(this::findSuccessResponse)
                .map(ApiResponse::getContent)
                .filter(content -> !content.isEmpty())
                .map(this::describe)
                .orElseGet(ResolvedResponse::empty);
    }

    private Optional<ApiResponse> findSuccessResponse(ApiResponses responses) {
        return responses.entrySet().stream()
                .filter(response -> isSuccessful(response.getKey()))
                .min(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .or(() -> Optional.ofNullable(responses.get(DEFAULT_STATUS)));
    }

    private boolean isSuccessful(String status) {
        return status.length() == 3 && status.charAt(0) == '2';
    }

    private ResolvedResponse describe(Content content) {
        var mediaTypes = List.copyOf(content.keySet());
        var schema = content.values().stream()
                .map(MediaType::getSchema)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new ResolvedResponse(schema, mediaTypes);
    }
}
