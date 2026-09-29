package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;

import java.util.List;

record ResolvedResponse(Schema<?> schema, List<String> mediaTypes) {

    static ResolvedResponse empty() {
        return new ResolvedResponse(null, List.of());
    }
}
