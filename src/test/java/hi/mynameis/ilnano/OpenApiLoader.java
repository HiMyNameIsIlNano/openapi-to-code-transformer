package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import static org.assertj.core.api.Assertions.assertThat;

interface OpenApiLoader {

    default OpenAPI load(String resource) {
        var api = new OpenApiParser(new OpenAPIV3Parser())
                .parse(resource);

        assertThat(api).as("Cannot parse %s", resource)
                .isNotNull();

        return api;
    }

}
