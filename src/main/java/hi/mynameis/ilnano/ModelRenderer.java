package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.media.Schema;

interface ModelRenderer {

    String render(String schemaName, Schema<?> schema);
}
