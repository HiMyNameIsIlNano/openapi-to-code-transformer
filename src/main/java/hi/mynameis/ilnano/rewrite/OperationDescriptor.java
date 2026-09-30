package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;

import java.util.List;

/**
 * One operation of a generated interface, as described by its {@code @ApiAnnotation} marker.
 *
 * @param path      the full path from the OpenAPI document, before the interface prefix is removed
 * @param mediaTypes the produced media types, empty when the operation has no response body
 */
record OperationDescriptor(
        OperationTypeEnum method,
        ResourcePath path,
        List<String> mediaTypes,
        List<ParameterDescriptor> parameters) {
}
