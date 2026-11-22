package cc.eamon.open.flow.core.error;

/**
 * 循环节点配置或执行错误
 */
public class FlowLoopException extends FlowRuntimeException {

    private static final long serialVersionUID = 1L;

    public FlowLoopException(String flowId, String message) {
        super(flowId, message);
    }

    public FlowLoopException(String flowId, String message, Throwable cause) {
        super(flowId, message, cause);
    }
}
