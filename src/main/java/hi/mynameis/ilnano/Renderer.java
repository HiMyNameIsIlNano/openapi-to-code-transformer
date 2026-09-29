package hi.mynameis.ilnano;

import java.util.List;

interface Renderer {

    String render(ApiTag tag, List<OperationInfo> operations);
}
