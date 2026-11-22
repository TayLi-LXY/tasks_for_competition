package cc.eamon.open.flow.competition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 classpath:competition/catalog.json 加载赛历（认定表调研结果外置，避免写死在代码里）。
 */
public final class CompetitionCatalog {

    public static final class Item {
        public final String id;
        public final String name;
        public final String track;
        public final String classLevel;
        public final String window;
        public final int score;
        public final String note;

        Item(String id, String name, String track, String classLevel, String window, int score, String note) {
            this.id = id;
            this.name = name;
            this.track = track;
            this.classLevel = classLevel;
            this.window = window;
            this.score = score;
            this.note = note == null ? "" : note;
        }
    }

    private static volatile CompetitionCatalog INSTANCE;

    private final String window;
    private final List<Item> items;
    private final Map<String, Item> byId;

    private CompetitionCatalog(String window, List<Item> items) {
        this.window = window;
        this.items = Collections.unmodifiableList(items);
        Map<String, Item> map = new LinkedHashMap<String, Item>();
        for (Item item : items) {
            map.put(item.id, item);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static CompetitionCatalog get() {
        if (INSTANCE == null) {
            synchronized (CompetitionCatalog.class) {
                if (INSTANCE == null) {
                    INSTANCE = load();
                }
            }
        }
        return INSTANCE;
    }

    public String window() {
        return window;
    }

    public List<Item> items() {
        return items;
    }

    public Item get(String id) {
        return byId.get(id);
    }

    public Map<String, Integer> scoreIndex() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (Item item : items) {
            map.put(item.id, item.score);
        }
        return map;
    }

    public Map<String, String> nameIndex() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Item item : items) {
            map.put(item.id, item.name);
        }
        return map;
    }

    public String trackOf(String id) {
        Item item = byId.get(id);
        return item == null ? "" : item.track;
    }

    private static CompetitionCatalog load() {
        InputStream in = CompetitionCatalog.class.getClassLoader().getResourceAsStream("competition/catalog.json");
        if (in == null) {
            throw new IllegalStateException("缺少配置 competition/catalog.json");
        }
        try {
            String json = readAll(in);
            JSONObject root = JSON.parseObject(json);
            String window = root.getString("window");
            JSONArray arr = root.getJSONArray("items");
            List<Item> list = new ArrayList<Item>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Item(
                        o.getString("id"),
                        o.getString("name"),
                        o.getString("track"),
                        o.getString("classLevel"),
                        o.getString("window"),
                        o.getIntValue("score"),
                        o.getString("note")
                ));
            }
            System.out.println("[COMP] catalog loaded, items=" + list.size() + ", window=" + window);
            return new CompetitionCatalog(window, list);
        } catch (Exception e) {
            throw new IllegalStateException("读取 competition/catalog.json 失败", e);
        }
    }

    private static String readAll(InputStream in) throws Exception {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        reader.close();
        return sb.toString();
    }
}
