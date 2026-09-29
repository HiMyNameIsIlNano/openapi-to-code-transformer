package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;

public interface Generator {

    GeneratedSources generate(OpenAPI spec, String basePackage);

}
