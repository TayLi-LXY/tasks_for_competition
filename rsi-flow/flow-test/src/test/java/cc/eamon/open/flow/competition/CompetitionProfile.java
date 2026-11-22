package cc.eamon.open.flow.competition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 结构化选手画像入参。
 */
public final class CompetitionProfile {

    public final String grade;
    public final String motive;
    public final String skills;
    public final String hours;
    public final String note;

    public CompetitionProfile(String grade, String motive, String skills, String hours, String note) {
        this.grade = safe(grade);
        this.motive = safe(motive);
        this.skills = safe(skills);
        this.hours = safe(hours);
        this.note = note == null ? "" : note.trim();
    }

    public static CompetitionProfile fromJson(JSONObject req) {
        if (req == null) req = new JSONObject();
        String grade = req.getString("grade");
        String motive = req.getString("motive");
        Object skillsObj = req.get("skills");
        String skills;
        if (skillsObj instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Iterable<?>) skillsObj) {
                if (o == null) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(String.valueOf(o).trim());
            }
            skills = sb.toString();
        } else {
            skills = req.getString("skills");
        }
        String hours = req.getString("hours");
        String note = req.getString("note");
        return new CompetitionProfile(grade, motive, skills, hours, note);
    }

    public void validate() {
        if (grade.isEmpty()) throw new IllegalArgumentException("grade 不能为空");
        if (motive.isEmpty()) throw new IllegalArgumentException("motive 不能为空");
        if (hours.isEmpty()) throw new IllegalArgumentException("hours 不能为空");
    }

    public String toProfileJson() {
        JSONObject o = new JSONObject(true);
        o.put("grade", grade);
        o.put("motive", motive);
        o.put("skills", skills);
        o.put("hours", hours);
        o.put("note", note);
        return o.toJSONString();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
