package hi.mynameis.ilnano;

import java.util.List;

record OperationInfo(
        OperationTypeEnum httpMethod,
        String path,
        String operationId,
        ResolvedResponse response,
        List<ParamInfo> params) {
}
