package cc.eamon.open.flow.competition;

import cc.eamon.open.flow.config.constants.FlowConstants;
import cc.eamon.open.flow.core.FlowEngine;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 统一执行入口：flowId 固定为 FLOW_COMPETITION_RESEARCH。
 */
public final class CompetitionFlowService {

    public static final String FLOW_ID = "FLOW_COMPETITION_RESEARCH";

    private CompetitionFlowService() {
    }

    public static JSONObject run(CompetitionProfile profile) throws Exception {
        if (profile == null) {
            throw new IllegalArgumentException("profile 不能为空");
        }
        profile.validate();
        try {
            Map<String, Object> results = FlowEngine.instance()
                    .register(FlowConstants.SCOPE_DEFAULT, "grade", profile.grade)
                    .register(FlowConstants.SCOPE_DEFAULT, "motive", profile.motive)
                    .register(FlowConstants.SCOPE_DEFAULT, "skills", profile.skills)
                    .register(FlowConstants.SCOPE_DEFAULT, "hours", profile.hours)
                    .register(FlowConstants.SCOPE_DEFAULT, "note", profile.note)
                    .run(FLOW_ID)
                    .retrieveFlowResults(FLOW_ID);

            JSONObject out = new JSONObject(true);
            out.put("flowId", FLOW_ID);
            out.put("grade", profile.grade);
            out.put("motive", profile.motive);
            out.put("skills", profile.skills);
            out.put("hours", profile.hours);
            out.put("note", profile.note);
            out.put("goalType", results.get("goalType"));
            out.put("profileJson", results.get("profileJson"));
            out.put("rulePrimary", results.get("rulePrimary"));
            out.put("ruleBackup", results.get("ruleBackup"));
            out.put("primaryCompetitions", results.get("primaryCompetitions"));
            out.put("backupCompetitions", results.get("backupCompetitions"));
            out.put("explanation", results.get("explanation"));
            out.put("catalogWindow", results.get("catalogWindow"));
            out.put("llmUsed", intFlag(results.get("llmUsed")));
            out.put("adjusted", intFlag(results.get("adjusted")));
            return out;
        } finally {
            FlowEngine.instance().release();
        }
    }

    private static boolean intFlag(Object v) {
        return v instanceof Number && ((Number) v).intValue() == 1;
    }
}
