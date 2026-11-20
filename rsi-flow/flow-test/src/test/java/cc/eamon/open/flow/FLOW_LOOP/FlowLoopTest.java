package cc.eamon.open.flow.FLOW_LOOP;

import cc.eamon.open.flow.config.constants.FlowConstants;
import cc.eamon.open.flow.core.FlowEngine;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 循环结构：条件回边实现 while (i < times) 累加
 */
public class FlowLoopTest {

    @Test
    public void caseLoopSumToThree() throws Exception {
        Integer sum = (Integer) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "times", 3)
                .run("FLOW_LOOP")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "sum");
        Assert.assertEquals(sum, Integer.valueOf(6));
        FlowEngine.instance().release();
    }

    @Test
    public void caseLoopSumToTen() throws Exception {
        Integer sum = (Integer) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "times", 10)
                .run("FLOW_LOOP")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "sum");
        Assert.assertEquals(sum, Integer.valueOf(55));
        FlowEngine.instance().release();
    }

    @Test
    public void caseLoopOnce() throws Exception {
        Integer sum = (Integer) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "times", 1)
                .run("FLOW_LOOP")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "sum");
        Assert.assertEquals(sum, Integer.valueOf(1));
        FlowEngine.instance().release();
    }
}
