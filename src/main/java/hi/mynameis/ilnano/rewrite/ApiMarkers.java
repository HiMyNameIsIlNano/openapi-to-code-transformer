package hi.mynameis.ilnano.rewrite;

/**
 * The simple names and member names of the generator's marker annotations, as they appear in the
 * generated sources.
 */
final class ApiMarkers {

    static final String MARKER_PACKAGE = "hi.mynameis.ilnano";

    static final String API_INTERFACE = "ApiInterface";
    static final String API_ANNOTATION = "ApiAnnotation";
    static final String API_PARAM = "ApiParam";

    static final String OPERATION_TYPE_ENUM = "OperationTypeEnum";
    static final String PARAMETER_LOCATION = "ParameterLocation";

    static final String TAG = "tag";
    static final String TYPE = "type";
    static final String PATH = "path";
    static final String PRODUCES = "produces";
    static final String NAME = "name";
    static final String IN = "in";
    static final String REQUIRED = "required";

    private ApiMarkers() {
    }

    /**
     * The fully qualified name of a marker type, for {@code maybeRemoveImport}.
     */
    static String qualified(String simpleName) {
        return MARKER_PACKAGE + "." + simpleName;
    }
}
