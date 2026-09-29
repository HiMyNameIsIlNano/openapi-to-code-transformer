package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;

record ParamInfo(String name, ParameterLocation in, Schema<?> schema, boolean required) {
}
