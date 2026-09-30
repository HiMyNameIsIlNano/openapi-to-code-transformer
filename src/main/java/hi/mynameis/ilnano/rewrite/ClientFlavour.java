package hi.mynameis.ilnano.rewrite;

import java.util.List;

/**
 * What one target framework needs written onto a generated interface.
 *
 * <p>Everything else — finding the markers, hoisting the common path prefix, replacing the
 * annotations, fixing the imports — is shared by {@link RewriteGeneratedApi}. A flavour only says
 * which annotations to render.
 *
 * <p>Every method returns annotation source without a trailing newline, and the imports that source
 * needs. Implementations must keep the two in step: an annotation whose import is missing is
 * written as an unresolvable symbol.
 */
interface ClientFlavour {

    /**
     * The annotations for the interface declaration, for example
     * {@code @RegisterRestClient(configKey = "pets-api")} plus {@code @Path("/pets")}.
     *
     * @param tag        the value of the {@code @ApiInterface} marker
     * @param commonPath the path prefix shared by every operation, possibly empty
     */
    List<String> interfaceAnnotations(String tag, ResourcePath commonPath);

    /**
     * The annotations for one operation, for example {@code @GET} plus {@code @Path("/{petId}")}.
     *
     * @param path the operation path with the interface prefix already removed, possibly empty
     */
    List<String> methodAnnotations(OperationDescriptor operation, ResourcePath path);

    /**
     * The annotation for one parameter, for example {@code @QueryParam("limit")}. An empty result
     * leaves the parameter unannotated, which is how a request body is expressed in flavours that
     * do not mark it.
     */
    List<String> parameterAnnotations(ParameterDescriptor parameter);

    /**
     * Every type the annotations above may reference, whether or not this particular interface uses
     * all of them. Handed to {@code maybeAddImport}, which only keeps the ones actually referenced.
     */
    List<String> imports();
}
