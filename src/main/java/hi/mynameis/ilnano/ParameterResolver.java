package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class ParameterResolver {

    private static final String BODY_PARAMETER_NAME = "body";

    List<ParamInfo> resolve(Operation operation) {
        return Stream.concat(declaredParameters(operation), bodyParameter(operation).stream())
                .toList();
    }

    private Stream<ParamInfo> declaredParameters(Operation operation) {
        return Optional.ofNullable(operation.getParameters())
                .orElseGet(List::of)
                .stream()
                .map(parameter -> new ParamInfo(
                        parameter.getName(),
                        ParameterLocation.from(parameter.getIn()),
                        parameter.getSchema(),
                        isRequired(parameter)));
    }

    private Optional<ParamInfo> bodyParameter(Operation operation) {
        return Optional.ofNullable(operation.getRequestBody())
                .flatMap(body -> bodySchema(body).map(schema -> new ParamInfo(
                        BODY_PARAMETER_NAME,
                        ParameterLocation.BODY,
                        schema,
                        Boolean.TRUE.equals(body.getRequired()))));
    }

    private Optional<Schema<?>> bodySchema(RequestBody body) {
        return Optional.ofNullable(body.getContent())
                .flatMap(content -> content.values().stream()
                        .<Schema<?>>map(MediaType::getSchema)
                        .filter(Objects::nonNull)
                        .findFirst());
    }

    private boolean isRequired(Parameter parameter) {
        return Boolean.TRUE.equals(parameter.getRequired());
    }
}
