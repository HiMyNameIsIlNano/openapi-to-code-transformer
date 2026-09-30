package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.OperationTypeEnum;

/**
 * A JAX-RS verb annotation for an HTTP method {@code jakarta.ws.rs} does not declare one for.
 *
 * <p>{@code jakarta.ws.rs} ships {@code @GET}, {@code @POST} and so on, but not every verb — there
 * is no {@code @TRACE}. {@code jakarta.ws.rs.HttpMethod} cannot stand in for the missing ones
 * because it targets {@code ANNOTATION_TYPE}: it is the meta-annotation used to *declare* a verb,
 * not one to put on a method. The spec's answer is to declare your own, which is what this writes:
 *
 * <pre>{@code
 * @Target(ElementType.METHOD)
 * @Retention(RetentionPolicy.RUNTIME)
 * @HttpMethod("TRACE")
 * public @interface TRACE {
 * }
 * }</pre>
 */
final class CustomVerbAnnotation {

    private CustomVerbAnnotation() {
    }

    /**
     * The simple name of the annotation for a verb, which is the verb itself.
     */
    static String nameOf(OperationTypeEnum method) {
        return method.name();
    }

    /**
     * The source of the annotation, in the package the interfaces using it live in.
     */
    static String sourceOf(OperationTypeEnum method, String apiPackage) {
        return """
                package %s;

                import jakarta.ws.rs.HttpMethod;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                /**
                 * The {@code %s} verb, which {@code jakarta.ws.rs} does not declare an annotation for.
                 */
                @Target(ElementType.METHOD)
                @Retention(RetentionPolicy.RUNTIME)
                @HttpMethod("%s")
                public @interface %s {
                }
                """.formatted(apiPackage, method.name(), method.name(), nameOf(method));
    }
}
