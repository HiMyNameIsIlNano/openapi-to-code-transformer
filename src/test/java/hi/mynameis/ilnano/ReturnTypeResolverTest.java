package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ReturnTypeResolverTest {

    private ReturnTypeResolver testSubject = new ReturnTypeResolver();

    @Test
    void no_response_should_return_void() {
        var emptyOperation = new Operation();

        var type = testSubject.resolve(emptyOperation);

        assertThat(type).isEqualTo("void");
    }

}