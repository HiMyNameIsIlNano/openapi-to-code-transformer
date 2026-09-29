package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.ArrayTypeName;
import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.ParameterizedTypeName;
import io.micronaut.sourcegen.javapoet.TypeName;
import io.swagger.v3.oas.models.media.Schema;

final class TypeMapper {

    private TypeMapper() {
        // To avoid instantiation
    }

    /**
     * Converts an OpenAPI schema into a JavaPoet TypeName.
     * Iterative: array nesting is handled by descending to the leaf schema,
     * then re-wrapping in List<...> once per array layer.
     */
    static TypeName toTypeName(Schema<?> schema, String modelPkg) {
        if (schema == null) {
            return TypeName.OBJECT;
        }

        // ---- 1. Descend through array layers to the leaf schema ----
        int arrayDepth = 0;
        Schema<?> leaf = schema;
        while (leaf != null && "array".equals(leaf.getType())) {
            arrayDepth++;
            leaf = leaf.getItems();       // step inward one level
            if (leaf == null) {           // malformed: array without items
                leaf = null;
                break;
            }
        }

        // ---- 2. Resolve the leaf (no recursion — flat resolution) ----
        TypeName resolved = resolveLeaf(leaf, modelPkg);

        // ---- 3. Re-wrap in List<...> once per array layer ----
        ClassName listType = ClassName.get("java.util", "List");
        for (int i = 0; i < arrayDepth; i++) {
            resolved = ParameterizedTypeName.get(listType, resolved);
        }
        return resolved;
    }

    /** Resolves a non-array schema to a Java type. Purely flat — no recursion. */
    private static TypeName resolveLeaf(Schema<?> schema, String modelPkg) {
        if (schema == null) {
            return TypeName.OBJECT;
        }

        // $ref → a ClassName in the model package (e.g. "#/.../Pet" -> Pet)
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String refName = ref.substring(ref.lastIndexOf('/') + 1);
            return ClassName.get(modelPkg, refName);
        }

        String type = schema.getType() == null ? "object" : schema.getType();
        String format = schema.getFormat();

        return switch (type) {
            case "integer" -> "int64".equals(format)
                    ? TypeName.get(Long.class)
                    : TypeName.get(Integer.class);
            case "number"  -> "float".equals(format)
                    ? TypeName.get(Float.class)
                    : TypeName.get(Double.class);
            case "boolean" -> TypeName.get(Boolean.class);
            case "string"  -> switch (format == null ? "" : format) {
                case "date-time" -> ClassName.get("java.time", "OffsetDateTime");
                case "date"      -> ClassName.get("java.time", "LocalDate");
                case "uuid"      -> TypeName.get(java.util.UUID.class);
                case "binary"    -> ArrayTypeName.of(TypeName.BYTE); // byte[]
                default          -> TypeName.get(String.class);
            };
            default -> TypeName.OBJECT; // "object" and anything unknown
        };
    }


}

