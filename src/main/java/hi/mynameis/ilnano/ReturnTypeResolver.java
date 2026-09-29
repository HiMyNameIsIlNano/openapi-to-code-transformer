package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;

final class ReturnTypeResolver {

    String resolve(Operation operation) {
        if (operation.getResponses() == null) {
            return "void";
        }

        var ok = operation.getResponses().get("200");
        if (ok == null) ok = operation.getResponses().get("default");
        if (ok == null || ok.getContent() == null) return "void";

        var json = ok.getContent().get("application/json");
        if (json == null || json.getSchema() == null) return "void";

        // return TypeMapper.toTypeName(json.getSchema(), "");   // handles $ref, arrays, primitives
        return "";
    }

}
