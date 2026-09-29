package hi.mynameis.ilnano;

import io.micronaut.sourcegen.javapoet.ArrayTypeName;
import io.micronaut.sourcegen.javapoet.ClassName;
import io.micronaut.sourcegen.javapoet.ParameterizedTypeName;
import io.micronaut.sourcegen.javapoet.TypeName;
import io.swagger.v3.oas.models.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class TypeMapper {

    private static final ClassName LIST = ClassName.get(List.class);

    private static final Map<SchemaType, Map<SchemaFormat, TypeName>> BY_TYPE_AND_FORMAT = Map.of(
            SchemaType.INTEGER, Map.of(SchemaFormat.INT64, TypeName.get(Long.class)),
            SchemaType.NUMBER, Map.of(SchemaFormat.FLOAT, TypeName.get(Float.class)),
            SchemaType.STRING, Map.of(
                    SchemaFormat.DATE, ClassName.get(LocalDate.class),
                    SchemaFormat.DATE_TIME, ClassName.get(OffsetDateTime.class),
                    SchemaFormat.UUID, ClassName.get(UUID.class),
                    SchemaFormat.BINARY, ArrayTypeName.of(TypeName.BYTE)));

    private static final Map<SchemaType, TypeName> BY_TYPE = Map.of(
            SchemaType.INTEGER, TypeName.get(Integer.class),
            SchemaType.NUMBER, TypeName.get(Double.class),
            SchemaType.BOOLEAN, TypeName.get(Boolean.class),
            SchemaType.STRING, TypeName.get(String.class),
            SchemaType.OBJECT, TypeName.OBJECT);

    private final String modelPackage;

    TypeMapper(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    TypeName toTypeName(Schema<?> schema) {
        return SchemaType.from(typeOf(schema)).isArray()
                ? ParameterizedTypeName.get(LIST, toTypeName(schema.getItems()))
                : toLeafTypeName(schema);
    }

    private TypeName toLeafTypeName(Schema<?> schema) {
        return Optional.ofNullable(schema)
                .map(Schema::get$ref)
                .<TypeName>map(this::toModelClassName)
                .orElseGet(() -> toBuiltInTypeName(schema));
    }

    private TypeName toBuiltInTypeName(Schema<?> schema) {
        var type = SchemaType.from(typeOf(schema));
        var format = SchemaFormat.from(schema == null ? null : schema.getFormat());

        return BY_TYPE_AND_FORMAT.getOrDefault(type, Map.of())
                .getOrDefault(format, BY_TYPE.getOrDefault(type, TypeName.OBJECT));
    }

    private ClassName toModelClassName(String reference) {
        return ClassName.get(modelPackage, reference.substring(reference.lastIndexOf('/') + 1));
    }

    private String typeOf(Schema<?> schema) {
        return schema == null ? null : schema.getType();
    }
}
