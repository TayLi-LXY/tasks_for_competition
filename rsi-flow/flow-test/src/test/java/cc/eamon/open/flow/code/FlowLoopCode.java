package cc.eamon.open.flow.code;

import cc.eamon.open.flow.core.FlowEngine;
import cc.eamon.open.flow.core.stereotype.Flow;

import java.util.List;

/**
 * 循环复合节点示例：while / do-while 累加，for 拼接
 */
public class FlowLoopCode {

    /** 兼容旧条件回边示例 */
    @Flow(value = "FLOW_LOOP_INIT")
    public void initLegacy(Integer times) {
        initWhile(times);
    }

    @Flow(value = "FLOW_LOOP_BODY")
    public void bodyLegacy(Integer i, Integer sum, Integer times) {
        bodyEvent(i, sum, times);
    }

    @Flow(value = "FLOW_LOOP_WHILE_INIT")
    public void initWhile(Integer times) {
        FlowEngine.instance().register("i", 0);
        FlowEngine.instance().register("sum", 0);
        FlowEngine.instance().register("times", times);
    }

    @Flow(value = "FLOW_LOOP_DO_WHILE_INIT")
    public void initDoWhile(Integer times) {
        FlowEngine.instance().register("i", 0);
        FlowEngine.instance().register("sum", 0);
        FlowEngine.instance().register("times", times);
    }

    @Flow(value = "FLOW_LOOP_BODY_EVENT")
    public void bodyEvent(Integer i, Integer sum, Integer times) {
        int next = i + 1;
        FlowEngine.instance().register("i", next);
        FlowEngine.instance().register("sum", sum + next);
        FlowEngine.instance().register("times", times);
    }

    @Flow(value = "FLOW_LOOP_FOR_INIT")
    public void initFor(List items) {
        FlowEngine.instance().register("joined", "");
        FlowEngine.instance().register("items", items);
    }

    @Flow(value = "FLOW_LOOP_FOR_BODY_EVENT")
    public void forBody(String item, String joined) {
        String next = (joined == null ? "" : joined) + (item == null ? "" : item);
        FlowEngine.instance().register("joined", next);
    }
}
