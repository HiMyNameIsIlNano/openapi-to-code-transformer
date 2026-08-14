package hi.mynameis.ilnano;

import java.util.List;

record OperationInfo(
        String httpMethod,
        String path,
        String operationId,
        String returnType,
        List<ParamInfo> params) {}

