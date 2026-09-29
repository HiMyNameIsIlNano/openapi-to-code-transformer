package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

final class OperationTypeEnumTest {

    @ParameterizedTest
    @EnumSource(HttpMethod.class)
    void every_swagger_http_method_maps_to_an_operation_type(HttpMethod method) {
        assertThat(OperationTypeEnum.from(method).name()).isEqualTo(method.name());
    }

    @ParameterizedTest
    @EnumSource(OperationTypeEnum.class)
    void every_operation_type_maps_back_to_a_swagger_http_method(OperationTypeEnum operation) {
        assertThat(HttpMethod.valueOf(operation.name())).isNotNull();
    }
}
