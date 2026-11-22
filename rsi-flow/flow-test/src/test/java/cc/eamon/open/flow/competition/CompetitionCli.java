package cc.eamon.open.flow.competition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * CLI 示例：
 * CompetitionCli --grade 大三 --motive 冲推免 --skills 算法,系统底层 --hours 10-20
 */
public class CompetitionCli {

    public static void main(String[] args) throws Exception {
        CompetitionProfile profile = parse(args);
        if (profile == null) {
            System.err.println("用法: CompetitionCli --grade 大三 --motive 冲推免 --skills 算法,系统底层 --hours 10-20 [--note ...]");
            System.exit(1);
        }
        System.out.println(JSON.toJSONString(CompetitionFlowService.run(profile), true));
    }

    private static CompetitionProfile parse(String[] args) {
        if (args == null || args.length == 0) {
            // 默认画像，方便快速试跑
            return new CompetitionProfile("大三", "冲推免", "算法", "10-20", "");
        }
        JSONObject o = new JSONObject(true);
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--") || i + 1 >= args.length) continue;
            String key = a.substring(2);
            String val = args[++i];
            o.put(key, val);
        }
        if (!o.containsKey("grade") || !o.containsKey("motive") || !o.containsKey("hours")) {
            return null;
        }
        if (!o.containsKey("skills")) o.put("skills", "");
        if (!o.containsKey("note")) o.put("note", "");
        return CompetitionProfile.fromJson(o);
    }
}
