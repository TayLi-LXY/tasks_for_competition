package cc.eamon.open.flow.code;

import cc.eamon.open.flow.competition.CompetitionCatalog;
import cc.eamon.open.flow.competition.CompetitionLlmClient;
import cc.eamon.open.flow.core.FlowEngine;
import cc.eamon.open.flow.core.stereotype.Flow;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 竞赛调研选型：结构化画像 → 规则保底 → LLM 可调整名单。
 */
public class FlowCompetitionCode {

    @Flow(value = "COMP_BUILD_PROFILE")
    public void buildProfile(String grade, String motive, String skills, String hours, String note) {
        String g = nz(grade);
        String m = nz(motive);
        String s = nz(skills);
        String h = nz(hours);
        String n = note == null ? "" : note.trim();

        String goalType = mapGoalType(m, s);

        JSONObject profile = new JSONObject(true);
        profile.put("grade", g);
        profile.put("motive", m);
        profile.put("skills", s);
        profile.put("hours", h);
        profile.put("note", n);
        String profileJson = profile.toJSONString();

        System.out.println("[COMP] BUILD_PROFILE " + profileJson + " -> goalType=" + goalType);
        FlowEngine.instance().register("profileJson", profileJson);
        FlowEngine.instance().register("goalType", goalType);
        FlowEngine.instance().register("grade", g);
        FlowEngine.instance().register("motive", m);
        FlowEngine.instance().register("skills", s);
        FlowEngine.instance().register("hours", h);
        FlowEngine.instance().register("note", n);
        FlowEngine.instance().register("llmEnabled", CompetitionLlmClient.enabled() ? 1 : 0);
    }

    @Flow(value = "COMP_LOAD_RULES")
    public void loadRules() {
        CompetitionCatalog catalog = CompetitionCatalog.get();
        System.out.println("[COMP] LOAD_RULES window=" + catalog.window() + " size=" + catalog.items().size());
        FlowEngine.instance().register("rulesReady", 1);
        FlowEngine.instance().register("catalogWindow", catalog.window());
    }

    @Flow(value = "COMP_FILTER_COLLEGE")
    public void filterCollege(String goalType) {
        List<String> ids = new ArrayList<String>();
        for (CompetitionCatalog.Item item : CompetitionCatalog.get().items()) {
            ids.add(item.id);
        }
        String pool = join(ids);
        System.out.println("[COMP] FILTER_COLLEGE goalType=" + goalType + " pool=" + pool);
        FlowEngine.instance().register("candidatePool", pool);
    }

    @Flow(value = "COMP_FILTER_TIME")
    public void filterTime(String candidatePool) {
        String window = CompetitionCatalog.get().window();
        System.out.println("[COMP] FILTER_TIME window=" + window + " pool=" + candidatePool);
        FlowEngine.instance().register("timeWindowPool", candidatePool == null ? "" : candidatePool);
    }

    @Flow(value = "COMP_EVAL_CLASS1")
    public void evalClass1(String timeWindowPool) {
        String pick = pickByTrack(timeWindowPool, "class1", 3);
        System.out.println("[COMP] EVAL_CLASS1 -> " + pick);
        FlowEngine.instance().register("class1Pick", pick);
    }

    @Flow(value = "COMP_EVAL_SYSTEM")
    public void evalSystem(String timeWindowPool) {
        String pick = pickByTrack(timeWindowPool, "system", 3);
        System.out.println("[COMP] EVAL_SYSTEM -> " + pick);
        FlowEngine.instance().register("systemPick", pick);
    }

    @Flow(value = "COMP_EVAL_ALGO")
    public void evalAlgo(String timeWindowPool) {
        String pick = pickByTrack(timeWindowPool, "algo", 3);
        System.out.println("[COMP] EVAL_ALGO -> " + pick);
        FlowEngine.instance().register("algoPick", pick);
    }

    @Flow(value = "COMP_EVAL_MODEL")
    public void evalModel(String timeWindowPool) {
        String pick = pickByTrack(timeWindowPool, "model", 3);
        System.out.println("[COMP] EVAL_MODEL -> " + pick);
        FlowEngine.instance().register("modelPick", pick);
    }

    @Flow(value = "COMP_RANK_OUTPUT")
    public void rankOutput(String goalType, String class1Pick, String systemPick,
                           String algoPick, String modelPick,
                           String grade, String motive, String skills, String hours) {
        CompetitionCatalog catalog = CompetitionCatalog.get();
        Map<String, Integer> baseScore = catalog.scoreIndex();
        Map<String, Integer> scoreById = applyProfileWeights(baseScore, catalog, goalType, grade, motive, skills, hours);
        Map<String, String> nameById = catalog.nameIndex();

        List<String> orderedTracks;
        if ("推免".equals(goalType)) {
            orderedTracks = Arrays.asList(class1Pick, systemPick, modelPick, algoPick);
        } else if ("系统".equals(goalType)) {
            orderedTracks = Arrays.asList(systemPick, class1Pick, algoPick, modelPick);
        } else if ("算法".equals(goalType)) {
            orderedTracks = Arrays.asList(algoPick, class1Pick, modelPick, systemPick);
        } else if ("建模".equals(goalType)) {
            orderedTracks = Arrays.asList(modelPick, class1Pick, systemPick, algoPick);
        } else {
            orderedTracks = Arrays.asList(class1Pick, systemPick, algoPick, modelPick);
        }

        List<String> ranked = new ArrayList<String>();
        for (String group : orderedTracks) {
            if (group == null || group.trim().isEmpty() || "-".equals(group)) continue;
            for (String id : group.split(",")) {
                id = id.trim();
                if (id.isEmpty() || ranked.contains(id)) continue;
                ranked.add(id);
            }
        }

        ranked = prioritize(goalType, ranked, scoreById, catalog);

        List<String> primaryIds = new ArrayList<String>();
        List<String> backupIds = new ArrayList<String>();
        for (int i = 0; i < ranked.size(); i++) {
            if (i < 2) primaryIds.add(ranked.get(i));
            else if (i < 5) backupIds.add(ranked.get(i));
        }

        String primary = toNames(primaryIds, nameById);
        String backup = toNames(backupIds, nameById);
        System.out.println("[COMP] RANK_OUTPUT goalType=" + goalType);
        System.out.println("[COMP]   rulePrimary=" + primary);
        System.out.println("[COMP]   ruleBackup =" + backup);
        FlowEngine.instance().register("rulePrimary", primary);
        FlowEngine.instance().register("ruleBackup", backup);
        FlowEngine.instance().register("primaryCompetitions", primary);
        FlowEngine.instance().register("backupCompetitions", backup);
    }

    @Flow(value = "COMP_LLM_ADJUST")
    public void llmAdjust(String profileJson, String goalType, String rulePrimary, String ruleBackup) {
        String primary = rulePrimary == null ? "-" : rulePrimary;
        String backup = ruleBackup == null ? "-" : ruleBackup;
        String explanation;
        boolean llmUsed = false;
        boolean adjusted = false;

        CompetitionLlmClient.AdjustResult adj = null;
        if (CompetitionLlmClient.enabled()) {
            adj = CompetitionLlmClient.adjust(profileJson, goalType, primary, backup);
        }

        if (adj != null && adj.primary != null && !adj.primary.isEmpty()) {
            String newPrimary = join(adj.primary);
            String newBackup = adj.backup == null || adj.backup.isEmpty() ? "-" : join(adj.backup);
            adjusted = !newPrimary.equals(primary) || !newBackup.equals(backup == null ? "-" : backup);
            primary = newPrimary;
            backup = newBackup;
            explanation = adj.explanation;
            llmUsed = true;
        } else {
            explanation = ruleExplain(goalType, primary, backup);
        }

        System.out.println("[COMP] LLM_ADJUST llmUsed=" + llmUsed + " adjusted=" + adjusted);
        System.out.println("[COMP]   primary=" + primary);
        System.out.println("[COMP]   backup =" + backup);
        FlowEngine.instance().register("primaryCompetitions", primary);
        FlowEngine.instance().register("backupCompetitions", backup);
        FlowEngine.instance().register("explanation", explanation);
        FlowEngine.instance().register("llmUsed", llmUsed ? 1 : 0);
        FlowEngine.instance().register("adjusted", adjusted ? 1 : 0);
        // 保留保底快照
        FlowEngine.instance().register("rulePrimary", rulePrimary);
        FlowEngine.instance().register("ruleBackup", ruleBackup);
    }

    private static String mapGoalType(String motive, String skills) {
        String m = motive == null ? "" : motive;
        String s = skills == null ? "" : skills;
        if (m.contains("推免")) return "推免";
        if (s.contains("系统")) return "系统";
        if (s.contains("算法")) return "算法";
        if (s.contains("建模")) return "建模";
        if (s.contains("硬件")) return "推免"; // 电设向，偏Ⅰ类
        if (s.contains("项目")) return "建模";
        if (m.contains("能力")) {
            if (s.contains("系统")) return "系统";
            if (s.contains("算法")) return "算法";
            return "综合";
        }
        if (m.contains("简历")) return "综合";
        return "综合";
    }

    private static Map<String, Integer> applyProfileWeights(
            Map<String, Integer> base,
            CompetitionCatalog catalog,
            String goalType, String grade, String motive, String skills, String hours) {
        Map<String, Integer> out = new HashMap<String, Integer>(base);
        String sk = skills == null ? "" : skills;
        String hr = hours == null ? "" : hours;
        String gr = grade == null ? "" : grade;
        boolean lowHours = hr.contains("5以下") || hr.equals("5以下");
        boolean midLowHours = hr.contains("5-10");
        boolean junior = gr.contains("大一") || gr.contains("大二");

        for (Map.Entry<String, Integer> e : base.entrySet()) {
            String id = e.getKey();
            int score = e.getValue();
            String track = catalog.trackOf(id);

            if (sk.contains("系统") && "system".equals(track)) score += 12;
            if (sk.contains("算法") && "algo".equals(track)) score += 12;
            if (sk.contains("建模") && "model".equals(track)) score += 10;
            if (sk.contains("硬件") && ("challenge".equals(id) || "nuedc".equals(id))) score += 10;
            if (sk.contains("项目") && ("service".equals(id) || "design".equals(id) || "challenge".equals(id))) score += 8;

            if (motive != null && motive.contains("推免") && "class1".equals(track)) score += 8;

            // 低投入：压高投入赛，抬短周期/可单人
            if (lowHours || midLowHours) {
                if ("challenge".equals(id) || "asc".equals(id) || "syscap".equals(id)) {
                    score -= lowHours ? 18 : 10;
                }
                if ("lanqiao".equals(id) || "mcm_us".equals(id) || "ccpc".equals(id)) {
                    score += lowHours ? 10 : 5;
                }
            }

            if (junior && "lanqiao".equals(id)) score += 8;

            out.put(id, score);
        }
        return out;
    }

    private static String ruleExplain(String goalType, String primary, String backup) {
        return "按画像映射目标「" + goalType + "」在认定表认可赛事中筛选 "
                + CompetitionCatalog.get().window() + " 窗口；"
                + "规则保底主赛：" + primary + "；备赛：" + backup
                + "。未启用 LLM 或调整失败时保留该名单。";
    }

    private static List<String> prioritize(String goalType, List<String> ranked,
                                           Map<String, Integer> scoreById,
                                           CompetitionCatalog catalog) {
        List<String> preferred = new ArrayList<String>();
        List<String> rest = new ArrayList<String>();
        for (String id : ranked) {
            String track = catalog.trackOf(id);
            boolean hit = false;
            if ("推免".equals(goalType)) hit = "class1".equals(track);
            else if ("系统".equals(goalType)) hit = "system".equals(track);
            else if ("算法".equals(goalType)) hit = "algo".equals(track);
            else if ("建模".equals(goalType)) hit = "model".equals(track);
            if (hit) preferred.add(id);
            else rest.add(id);
        }
        preferred.sort((a, b) -> Integer.compare(
                scoreById.containsKey(b) ? scoreById.get(b) : 0,
                scoreById.containsKey(a) ? scoreById.get(a) : 0));
        rest.sort((a, b) -> Integer.compare(
                scoreById.containsKey(b) ? scoreById.get(b) : 0,
                scoreById.containsKey(a) ? scoreById.get(a) : 0));
        List<String> out = new ArrayList<String>();
        out.addAll(preferred);
        out.addAll(rest);
        return out;
    }

    private static String pickByTrack(String pool, String track, int limit) {
        List<String> allowed = splitPool(pool);
        List<CompetitionCatalog.Item> matched = new ArrayList<CompetitionCatalog.Item>();
        for (CompetitionCatalog.Item item : CompetitionCatalog.get().items()) {
            if (!track.equals(item.track)) continue;
            if (!allowed.isEmpty() && !allowed.contains(item.id)) continue;
            matched.add(item);
        }
        matched.sort((a, b) -> Integer.compare(b.score, a.score));
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < matched.size() && i < limit; i++) {
            ids.add(matched.get(i).id);
        }
        return ids.isEmpty() ? "-" : join(ids);
    }

    private static List<String> splitPool(String pool) {
        List<String> list = new ArrayList<String>();
        if (pool == null || pool.trim().isEmpty()) return list;
        for (String s : pool.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    private static String toNames(List<String> ids, Map<String, String> nameById) {
        if (ids == null || ids.isEmpty()) return "-";
        List<String> names = new ArrayList<String>();
        for (String id : ids) {
            names.add(nameById.containsKey(id) ? nameById.get(id) : id);
        }
        return join(names);
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
