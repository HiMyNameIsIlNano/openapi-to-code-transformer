package hi.mynameis.ilnano;

import java.util.List;

/**
 * It is a collection of metadata for the given operation
 **/
record OperationInfo(
        String httpMethod,
        String path,
        String operationId,
        String returnType,
        List<ParamInfo> params) {}

