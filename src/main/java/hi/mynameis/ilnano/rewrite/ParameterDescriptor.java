package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.ParameterLocation;

/**
 * One method parameter of a generated interface, as described by its {@code @ApiParam} marker.
 *
 * @param specName   the name from the OpenAPI document, which is what the wire uses
 * @param javaName   the parameter name in the generated source
 * @param typeSource the declared type, printed from the source so that generics and imports are
 *                   preserved exactly as the generator wrote them
 */
record ParameterDescriptor(
        String specName,
        String javaName,
        String typeSource,
        ParameterLocation in,
        boolean required) {

    boolean isBody() {
        return in == ParameterLocation.BODY;
    }
}
