package cc.eamon.open.flow.core.stereotype.execute;

import cc.eamon.open.flow.config.constants.FlowConstants;
import cc.eamon.open.flow.config.flow.FlowConfig;
import cc.eamon.open.flow.config.flow.FlowNodeConfig;
import cc.eamon.open.flow.config.flow.node.FlowParamConfig;
import cc.eamon.open.flow.config.flow.node.FlowResultConfig;
import cc.eamon.open.flow.core.FlowEngine;
import cc.eamon.open.flow.core.context.FlowContextHolder;
import cc.eamon.open.flow.core.context.InitializerEnums;
import cc.eamon.open.flow.core.error.FlowLoopException;
import cc.eamon.open.flow.core.error.FlowNestParamNotExistException;
import cc.eamon.open.flow.core.error.FlowNotFoundException;
import cc.eamon.open.flow.core.stereotype.execute.holder.FlowHolder;
import cc.eamon.open.flow.core.stereotype.execute.holder.FlowParamHolder;
import cc.eamon.open.flow.core.stereotype.execute.holder.FlowResultHolder;
import cc.eamon.open.flow.model.ModelEngine;
import cc.eamon.open.flow.model.enums.TypeEnums;
import cc.eamon.open.flow.route.util.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 循环复合节点：reference 指向循环体子流程，按 loopMode 反复执行。
 * 状态写入作用域 loop:{nodeId}
 */
public class LoopExecutable implements Executable {

    private static final Logger logger = LoggerFactory.getLogger(LoopExecutable.class);

    public static final String MODE_FOR = "for";
    public static final String MODE_WHILE = "while";
    public static final String MODE_DO_WHILE = "do-while";

    public static final String ON_ERROR_THROW = "throw";
    public static final String ON_ERROR_BREAK = "break";
    public static final String ON_ERROR_CONTINUE = "continue";

    public static final String ITEM_VAR = "item";
    public static final String INDEX_VAR = "index";

    private final FlowNodeConfig flowNodeConfig;
    private FlowHolder flowHolder;

    public LoopExecutable(FlowNodeConfig flowNodeConfig) {
        this.flowNodeConfig = flowNodeConfig;
        this.flowHolder = InitializerEnums.DEFAULT.getInitializer().getFlowHolder(flowNodeConfig.getReference());
    }

    @Override
    public List<TypeEnums> getParamsTypes() {
        ensureBody();
        return flowHolder.getParamsTypes();
    }

    @Override
    public void execute() throws Exception {
        ensureBody();
        validateConfig();

        String mode = normalizeMode(flowNodeConfig.getLoopMode());
        String onError = normalizeOnError(flowNodeConfig.getOnError());
        int maxIterations = flowNodeConfig.getMaxIterations();
        Long timeoutMs = flowNodeConfig.getTimeoutMs();
        long startAt = System.currentTimeMillis();
        String statusScope = "loop:" + flowNodeConfig.getId();

        writeStatus(statusScope, mode, 0, "running", null);

        int iterations = 0;
        try {
            if (MODE_FOR.equals(mode)) {
                iterations = runFor(maxIterations, timeoutMs, startAt, onError, statusScope);
            } else if (MODE_DO_WHILE.equals(mode)) {
                iterations = runDoWhile(maxIterations, timeoutMs, startAt, onError, statusScope);
            } else {
                iterations = runWhile(maxIterations, timeoutMs, startAt, onError, statusScope);
            }
            writeStatus(statusScope, mode, iterations, "completed", null);
        } catch (FlowLoopException e) {
            writeStatus(statusScope, mode, iterations, "error", e.getMessage());
            throw e;
        } catch (Exception e) {
            writeStatus(statusScope, mode, iterations, "error", e.getMessage());
            throw e;
        }
    }

    private int runWhile(int maxIterations, Long timeoutMs, long startAt, String onError, String statusScope) throws Exception {
        int iterations = 0;
        while (evalCondition()) {
            checkTimeout(timeoutMs, startAt);
            if (iterations >= maxIterations) {
                throw new FlowLoopException(flowNodeConfig.getId(),
                        "exceeded maxIterations=" + maxIterations);
            }
            if (!runOneIteration(iterations, onError)) {
                break;
            }
            iterations++;
            writeStatus(statusScope, MODE_WHILE, iterations, "running", null);
        }
        return iterations;
    }

    private int runDoWhile(int maxIterations, Long timeoutMs, long startAt, String onError, String statusScope) throws Exception {
        int iterations = 0;
        do {
            checkTimeout(timeoutMs, startAt);
            if (iterations >= maxIterations) {
                throw new FlowLoopException(flowNodeConfig.getId(),
                        "exceeded maxIterations=" + maxIterations);
            }
            if (!runOneIteration(iterations, onError)) {
                break;
            }
            iterations++;
            writeStatus(statusScope, MODE_DO_WHILE, iterations, "running", null);
        } while (evalCondition());
        return iterations;
    }

    private int runFor(int maxIterations, Long timeoutMs, long startAt, String onError, String statusScope) throws Exception {
        String iterationKey = flowNodeConfig.getIterationKey();
        if (iterationKey == null || iterationKey.trim().isEmpty()) {
            throw new FlowLoopException(flowNodeConfig.getId(), "for mode requires iterationKey");
        }
        Object collection = FlowContextHolder.get().retrieveVariable(FlowConstants.SCOPE_DEFAULT, iterationKey);
        if (collection == null) {
            collection = FlowContextHolder.get().retrieveVariable(iterationKey);
        }
        List<Object> items = toItemList(collection);
        int iterations = 0;
        for (Object item : items) {
            checkTimeout(timeoutMs, startAt);
            if (iterations >= maxIterations) {
                throw new FlowLoopException(flowNodeConfig.getId(),
                        "exceeded maxIterations=" + maxIterations);
            }
            FlowContextHolder.get().registerVariable(FlowConstants.SCOPE_DEFAULT, ITEM_VAR, item);
            FlowContextHolder.get().registerVariable(FlowConstants.SCOPE_DEFAULT, INDEX_VAR, iterations);
            FlowContextHolder.get().registerVariable(statusScope, ITEM_VAR, item);
            FlowContextHolder.get().registerVariable(statusScope, INDEX_VAR, iterations);

            if (!runOneIteration(iterations, onError)) {
                break;
            }
            iterations++;
            writeStatus(statusScope, MODE_FOR, iterations, "running", null);
        }
        return iterations;
    }

    /**
     * @return false 表示应中断循环（onError=break）
     */
    private boolean runOneIteration(int index, String onError) throws Exception {
        try {
            bindParams();
            String currentScope = flowNodeConfig.getReference();
            FlowEngine.instance().setFlowScope(currentScope);
            flowHolder.execute();
            bindResults();
            return true;
        } catch (Exception e) {
            logger.info("loop node {} iteration {} error: {}", flowNodeConfig.getId(), index, e.getMessage());
            if (ON_ERROR_BREAK.equals(onError)) {
                return false;
            }
            if (ON_ERROR_CONTINUE.equals(onError)) {
                return true;
            }
            throw new FlowLoopException(flowNodeConfig.getId(),
                    "iteration " + index + " failed: " + e.getMessage(), e);
        }
    }

    private void bindParams() {
        String currentScope = flowNodeConfig.getReference();
        for (int i = 0; i < flowNodeConfig.getParams().size(); i++) {
            FlowParamConfig flowParamConfig = flowNodeConfig.getParams().get(i);
            FlowParamHolder flowParamHolder = flowHolder.getParams().get(i);
            Object variable = FlowContextHolder.get().retrieveVariable(flowParamConfig.getScope(), flowParamConfig.getName());
            Object convertedVariable = ModelEngine.build(
                    flowParamConfig.getName(),
                    flowParamConfig.getType(),
                    variable,
                    FlowContextHolder.get().retrieveVariables(currentScope));
            if (convertedVariable == null) {
                if (flowParamHolder.getNotNull()) {
                    throw new FlowNestParamNotExistException(
                            flowNodeConfig.getId(), flowParamHolder.getScope(), flowParamHolder.getName());
                } else {
                    if (flowParamHolder.getDefaultValue() != null) {
                        convertedVariable = flowParamHolder.getDefaultValue();
                    } else {
                        continue;
                    }
                }
            }
            FlowContextHolder.get().registerVariable(
                    flowParamHolder.getScope(), flowParamHolder.getName(), convertedVariable);
        }
    }

    private void bindResults() {
        for (int i = 0; i < flowNodeConfig.getResults().size(); i++) {
            FlowResultConfig flowResultConfig = flowNodeConfig.getResults().get(i);
            FlowResultHolder flowResultHolder = flowHolder.getResults().get(i);
            Object variable = FlowContextHolder.get().retrieveVariable(
                    flowResultHolder.getScope(), flowResultHolder.getName());
            Object convertedVariable = ModelEngine.build(
                    flowResultConfig.getName(),
                    flowResultConfig.getType(),
                    variable,
                    FlowContextHolder.get().retrieveVariables(flowResultConfig.getScope()));
            if (convertedVariable == null) {
                if (flowResultHolder.getNotNull()) {
                    throw new FlowNestParamNotExistException(
                            flowNodeConfig.getId(), flowResultConfig.getScope(), flowResultConfig.getName());
                } else {
                    continue;
                }
            }
            FlowContextHolder.get().registerVariable(
                    flowResultConfig.getScope(), flowResultConfig.getName(), convertedVariable);
        }
    }

    private boolean evalCondition() {
        String condition = flowNodeConfig.getCondition();
        if (condition == null || condition.trim().isEmpty()) {
            throw new FlowLoopException(flowNodeConfig.getId(), "while/do-while requires condition");
        }
        Map<String, Object> variables = new HashMap<>(FlowContextHolder.get().retrieveVariables());
        // 父流程作用域中的循环变量（i/sum 等）也要参与条件求值
        FlowConfig parent = InitializerEnums.DEFAULT.getInitializer()
                .getFlowNodeConfigFlowConfigs().get(flowNodeConfig);
        if (parent != null) {
            variables.putAll(FlowContextHolder.get().retrieveVariables(parent.getId()));
        }
        Object result = JexlUtils.invokeMethod(condition, variables);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        if (result == null || "".equals(result)) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(result));
    }

    private void validateConfig() {
        Integer maxIterations = flowNodeConfig.getMaxIterations();
        if (maxIterations == null || maxIterations <= 0) {
            throw new FlowLoopException(flowNodeConfig.getId(),
                    "maxIterations is required and must be > 0");
        }
        String mode = flowNodeConfig.getLoopMode();
        if (mode == null || mode.trim().isEmpty()) {
            throw new FlowLoopException(flowNodeConfig.getId(), "loopMode is required");
        }
        normalizeMode(mode);
    }

    private String normalizeMode(String mode) {
        String m = mode.trim().toLowerCase();
        if (MODE_FOR.equals(m) || MODE_WHILE.equals(m) || MODE_DO_WHILE.equals(m)) {
            return m;
        }
        throw new FlowLoopException(flowNodeConfig.getId(),
                "unsupported loopMode: " + mode + " (expected for|while|do-while)");
    }

    private String normalizeOnError(String onError) {
        if (onError == null || onError.trim().isEmpty()) {
            return ON_ERROR_THROW;
        }
        String v = onError.trim().toLowerCase();
        if (ON_ERROR_THROW.equals(v) || ON_ERROR_BREAK.equals(v) || ON_ERROR_CONTINUE.equals(v)) {
            return v;
        }
        throw new FlowLoopException(flowNodeConfig.getId(),
                "unsupported onError: " + onError + " (expected throw|break|continue)");
    }

    private void checkTimeout(Long timeoutMs, long startAt) {
        if (timeoutMs == null) {
            return;
        }
        if (timeoutMs <= 0) {
            throw new FlowLoopException(flowNodeConfig.getId(), "timeoutMs must be > 0 when set");
        }
        long elapsed = System.currentTimeMillis() - startAt;
        if (elapsed > timeoutMs) {
            throw new FlowLoopException(flowNodeConfig.getId(),
                    "loop timeout after " + elapsed + "ms (limit " + timeoutMs + "ms)");
        }
    }

    private void writeStatus(String statusScope, String mode, int iteration, String status, String message) {
        FlowContextHolder.get().registerVariable(statusScope, "mode", mode);
        FlowContextHolder.get().registerVariable(statusScope, "iteration", iteration);
        FlowContextHolder.get().registerVariable(statusScope, "status", status);
        if (message != null) {
            FlowContextHolder.get().registerVariable(statusScope, "message", message);
        }
        FlowContextHolder.get().registerVariable(statusScope, "nodeId", flowNodeConfig.getId());
    }

    @SuppressWarnings("unchecked")
    private List<Object> toItemList(Object collection) {
        List<Object> items = new ArrayList<>();
        if (collection == null) {
            return items;
        }
        if (collection instanceof Collection) {
            items.addAll((Collection<Object>) collection);
            return items;
        }
        if (collection instanceof Map) {
            items.addAll(((Map<?, ?>) collection).entrySet());
            return items;
        }
        if (collection.getClass().isArray()) {
            int len = Array.getLength(collection);
            for (int i = 0; i < len; i++) {
                items.add(Array.get(collection, i));
            }
            return items;
        }
        if (collection instanceof CharSequence) {
            CharSequence cs = (CharSequence) collection;
            for (int i = 0; i < cs.length(); i++) {
                items.add(String.valueOf(cs.charAt(i)));
            }
            return items;
        }
        if (collection instanceof Iterable) {
            Iterator<?> it = ((Iterable<?>) collection).iterator();
            while (it.hasNext()) {
                items.add(it.next());
            }
            return items;
        }
        throw new FlowLoopException(flowNodeConfig.getId(),
                "iterationKey value is not iterable: " + collection.getClass().getName());
    }

    private void ensureBody() {
        if (flowHolder == null) {
            this.flowHolder = InitializerEnums.DEFAULT.getInitializer()
                    .getFlowHolder(flowNodeConfig.getReference());
        }
        if (flowHolder == null) {
            throw new FlowNotFoundException(flowNodeConfig.getReference());
        }
    }
}
