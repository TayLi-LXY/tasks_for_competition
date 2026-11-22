package cc.eamon.open.flow.FLOW_COMPETITION_RESEARCH;

import cc.eamon.open.flow.competition.CompetitionFlowService;
import cc.eamon.open.flow.competition.CompetitionProfile;
import com.alibaba.fastjson.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 结构化画像入参；无 LLM 时断言规则保底结果。
 */
public class FlowCompetitionResearchTest {

    @Test
    public void casePushMian() throws Exception {
        JSONObject results = CompetitionFlowService.run(
                new CompetitionProfile("大三", "冲推免", "算法,硬件嵌入式", "10-20", ""));
        String primary = results.getString("primaryCompetitions");
        String rulePrimary = results.getString("rulePrimary");
        System.out.println("=== 冲推免 ===");
        System.out.println(results.toJSONString());
        Assert.assertEquals(results.getString("flowId"), CompetitionFlowService.FLOW_ID);
        Assert.assertEquals(results.getString("goalType"), "推免");
        Assert.assertTrue(
                rulePrimary.contains("挑战杯") || rulePrimary.contains("电子设计") || rulePrimary.contains("数学建模"),
                "推免规则保底应以Ⅰ类为主: " + rulePrimary);
        Assert.assertNotNull(primary);
        Assert.assertFalse(primary.isEmpty());
        Assert.assertNotNull(results.getString("explanation"));
        // 无 Key 时 final == rule
        if (!results.getBooleanValue("llmUsed")) {
            Assert.assertEquals(primary, rulePrimary);
            Assert.assertFalse(results.getBooleanValue("adjusted"));
        }
    }

    @Test
    public void caseSystemLowHours() throws Exception {
        JSONObject results = CompetitionFlowService.run(
                new CompetitionProfile("大二", "练能力", "系统底层", "5以下", "时间少"));
        String rulePrimary = results.getString("rulePrimary");
        System.out.println("=== 系统+低投入 ===");
        System.out.println(results.toJSONString());
        Assert.assertEquals(results.getString("goalType"), "系统");
        Assert.assertTrue(
                rulePrimary.contains("系统能力") || rulePrimary.contains("ASC") || rulePrimary.contains("软件"),
                "系统技能应以系统赛道为主: " + rulePrimary);
    }

    @Test
    public void caseAlgo() throws Exception {
        JSONObject results = CompetitionFlowService.run(
                new CompetitionProfile("大三", "练能力", "算法", "10-20", ""));
        String rulePrimary = results.getString("rulePrimary");
        System.out.println("=== 算法 ===");
        System.out.println(results.toJSONString());
        Assert.assertEquals(results.getString("goalType"), "算法");
        Assert.assertTrue(
                rulePrimary.contains("ICPC") || rulePrimary.contains("CCPC") || rulePrimary.contains("蓝桥"),
                "算法技能应以程序设计赛为主: " + rulePrimary);
    }
}
