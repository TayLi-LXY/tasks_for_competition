package cc.eamon.open.flow.code;

import cc.eamon.open.flow.core.FlowEngine;
import cc.eamon.open.flow.core.stereotype.Flow;

/**
 * 循环结构示例节点：计算 sum = 1 + 2 + ... + times
 */
public class FlowLoopCode {

    @Flow(value = "FLOW_LOOP_INIT")
    public void init(Integer times) {
        FlowEngine.instance().register("i", 0);
        FlowEngine.instance().register("sum", 0);
        FlowEngine.instance().register("times", times);
    }

    @Flow(value = "FLOW_LOOP_BODY")
    public void body(Integer i, Integer sum, Integer times) {
        int next = i + 1;
        FlowEngine.instance().register("i", next);
        FlowEngine.instance().register("sum", sum + next);
        FlowEngine.instance().register("times", times);
    }
}
