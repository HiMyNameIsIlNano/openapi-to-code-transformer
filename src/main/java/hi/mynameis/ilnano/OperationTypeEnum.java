package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.PathItem.HttpMethod;

public enum OperationTypeEnum {

    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    static OperationTypeEnum from(HttpMethod method) {
        return valueOf(method.name());
    }
}
