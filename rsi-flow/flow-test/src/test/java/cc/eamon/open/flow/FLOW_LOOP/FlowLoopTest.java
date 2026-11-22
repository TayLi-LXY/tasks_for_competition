package cc.eamon.open.flow.FLOW_LOOP;

import cc.eamon.open.flow.config.constants.FlowConstants;
import cc.eamon.open.flow.core.FlowEngine;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 循环复合节点：for / while / do-while
 */
public class FlowLoopTest {

    @Test
    public void caseWhileSumToThree() throws Exception {
        Integer sum = (Integer) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "times", 3)
                .run("FLOW_LOOP_WHILE")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "sum");
        Assert.assertEquals(sum, Integer.valueOf(6));
        FlowEngine.instance().release();
    }

    @Test
    public void caseDoWhileOnce() throws Exception {
        // times=1：do-while 至少执行一次，sum=1
        Integer sum = (Integer) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "times", 1)
                .run("FLOW_LOOP_DO_WHILE")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "sum");
        Assert.assertEquals(sum, Integer.valueOf(1));
        FlowEngine.instance().release();
    }

    @Test
    public void caseForJoinItems() throws Exception {
        List<String> items = Arrays.asList("a", "b", "c");
        String joined = (String) FlowEngine.instance()
                .register(FlowConstants.SCOPE_DEFAULT, "items", items)
                .run("FLOW_LOOP_FOR")
                .retrieve(FlowConstants.SCOPE_DEFAULT, "joined");
        Assert.assertEquals(joined, "abc");
        FlowEngine.instance().release();
    }
}
