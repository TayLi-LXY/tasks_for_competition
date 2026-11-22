package cc.eamon.open.flow.competition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 可选 LLM 客户端（OpenAI 兼容接口）。
 * 环境变量：
 *   COMPETITION_LLM_API_KEY  — 有值才启用
 *   COMPETITION_LLM_BASE_URL — 默认 https://maas.whu.edu.cn/v1
 *   COMPETITION_LLM_MODEL    — 默认 deepseek-v4-flash
 */
public final class CompetitionLlmClient {

    public static final class AdjustResult {
        public final List<String> primary;
        public final List<String> backup;
        public final String explanation;

        public AdjustResult(List<String> primary, List<String> backup, String explanation) {
            this.primary = primary;
            this.backup = backup;
            this.explanation = explanation;
        }
    }

    private CompetitionLlmClient() {
    }

    public static boolean enabled() {
        String key = System.getenv("COMPETITION_LLM_API_KEY");
        return key != null && !key.trim().isEmpty();
    }

    /**
     * 在规则保底名单上调整主赛/备赛，并生成解释。只允许 catalog 内赛事名。
     */
    public static AdjustResult adjust(String profileJson, String goalType,
                                      String rulePrimary, String ruleBackup) {
        if (!enabled()) return null;

        StringBuilder catalogBrief = new StringBuilder();
        Set<String> allowedNames = new LinkedHashSet<String>();
        for (CompetitionCatalog.Item item : CompetitionCatalog.get().items()) {
            allowedNames.add(item.name);
            catalogBrief.append("- ").append(item.name)
                    .append(" | track=").append(item.track)
                    .append(" | level=").append(item.classLevel)
                    .append(" | score=").append(item.score)
                    .append('\n');
        }

        String prompt = "你是武大计算机学院竞赛顾问。根据选手画像与规则引擎给出的保底名单，"
                + "可在「允许赛事列表」内微调主赛/备赛，并给出解释。\n"
                + "硬性约束：\n"
                + "1) 只能使用允许列表中的赛事全名；\n"
                + "2) primary 最多 2 个，backup 最多 3 个；\n"
                + "3) 只输出一个 JSON 对象，不要 markdown，不要其它文字，格式：\n"
                + "{\"primary\":[\"...\"],\"backup\":[\"...\"],\"explanation\":\"...\"}\n\n"
                + "选手画像：" + profileJson + "\n"
                + "内部目标类型：" + goalType + "\n"
                + "规则保底主赛：" + rulePrimary + "\n"
                + "规则保底备赛：" + ruleBackup + "\n"
                + "允许赛事列表：\n" + catalogBrief;

        String raw = chat(prompt);
        if (raw == null || raw.trim().isEmpty()) return null;

        try {
            String jsonText = extractJsonObject(raw);
            JSONObject obj = JSON.parseObject(jsonText);
            List<String> primary = sanitizeNames(obj.getJSONArray("primary"), allowedNames, 2);
            List<String> backup = sanitizeNames(obj.getJSONArray("backup"), allowedNames, 3);
            // 从 backup 去掉已在 primary 的
            List<String> backupClean = new ArrayList<String>();
            for (String b : backup) {
                if (!primary.contains(b)) backupClean.add(b);
            }
            String explanation = obj.getString("explanation");
            if (explanation == null || explanation.trim().isEmpty()) {
                explanation = "已根据画像对规则保底名单做微调。";
            }
            if (primary.isEmpty()) {
                return null;
            }
            return new AdjustResult(primary, backupClean, explanation.trim());
        } catch (Exception e) {
            System.out.println("[COMP][LLM] adjust parse failed: " + e.getMessage() + " raw=" + raw);
            return null;
        }
    }

    private static List<String> sanitizeNames(JSONArray arr, Set<String> allowed, int limit) {
        List<String> out = new ArrayList<String>();
        if (arr == null) return out;
        for (int i = 0; i < arr.size() && out.size() < limit; i++) {
            String name = arr.getString(i);
            if (name == null) continue;
            name = name.trim();
            String matched = matchAllowed(name, allowed);
            if (matched != null && !out.contains(matched)) {
                out.add(matched);
            }
        }
        return out;
    }

    private static String matchAllowed(String name, Set<String> allowed) {
        if (allowed.contains(name)) return name;
        for (String a : allowed) {
            if (name.contains(a) || a.contains(name)) return a;
        }
        return null;
    }

    private static String extractJsonObject(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('{');
            int end = s.lastIndexOf('}');
            if (start >= 0 && end > start) return s.substring(start, end + 1);
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s;
    }

    private static String chat(String userContent) {
        if (!enabled()) return null;
        try {
            String base = env("COMPETITION_LLM_BASE_URL", "https://maas.whu.edu.cn/v1");
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String model = env("COMPETITION_LLM_MODEL", "deepseek-v4-flash");
            String key = System.getenv("COMPETITION_LLM_API_KEY").trim();

            JSONObject body = new JSONObject();
            body.put("model", model);
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", userContent);
            messages.add(msg);
            body.put("messages", messages);
            body.put("temperature", 0.3);

            URL url = new URL(base + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + key);

            byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            if (code >= 400) {
                System.out.println("[COMP][LLM] http " + code + " " + sb);
                return null;
            }
            JSONObject resp = JSON.parseObject(sb.toString());
            return resp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            System.out.println("[COMP][LLM] failed: " + e.getMessage());
            return null;
        }
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return v == null || v.trim().isEmpty() ? def : v.trim();
    }
}
