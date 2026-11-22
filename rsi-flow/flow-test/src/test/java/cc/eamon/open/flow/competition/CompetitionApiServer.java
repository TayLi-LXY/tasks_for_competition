package cc.eamon.open.flow.competition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * 轻量 HTTP API。
 * POST /api/competition/run
 *   {"grade":"大三","motive":"冲推免","skills":["算法","系统底层"],"hours":"10-20","note":"..."}
 */
public class CompetitionApiServer {

    public static void main(String[] args) throws Exception {
        int port = 8088;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        CompetitionCatalog.get();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", CompetitionApiServer::health);
        server.createContext("/api/competition/catalog", CompetitionApiServer::catalog);
        server.createContext("/api/competition/run", CompetitionApiServer::run);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[COMP] API ready: http://localhost:" + port);
        System.out.println("[COMP] POST /api/competition/run  {grade,motive,skills,hours,note}");
        System.out.println("[COMP] flowId=" + CompetitionFlowService.FLOW_ID
                + " llm=" + CompetitionLlmClient.enabled());
    }

    private static void health(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        JSONObject o = new JSONObject(true);
        o.put("ok", true);
        o.put("flowId", CompetitionFlowService.FLOW_ID);
        o.put("llmEnabled", CompetitionLlmClient.enabled());
        writeJson(ex, 200, o);
    }

    private static void catalog(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            writeJson(ex, 405, err("method not allowed"));
            return;
        }
        CompetitionCatalog c = CompetitionCatalog.get();
        JSONObject o = new JSONObject(true);
        o.put("window", c.window());
        o.put("items", c.items());
        writeJson(ex, 200, o);
    }

    private static void run(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            writeJson(ex, 405, err("method not allowed"));
            return;
        }
        try {
            String body = readBody(ex.getRequestBody());
            JSONObject req = body == null || body.trim().isEmpty()
                    ? new JSONObject()
                    : JSON.parseObject(body);
            CompetitionProfile profile = CompetitionProfile.fromJson(req);
            JSONObject result = CompetitionFlowService.run(profile);
            writeJson(ex, 200, result);
        } catch (IllegalArgumentException e) {
            writeJson(ex, 400, err(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(ex, 500, err(e.getMessage()));
        }
    }

    private static void cors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static JSONObject err(String message) {
        JSONObject o = new JSONObject(true);
        o.put("error", message);
        return o;
    }

    private static void writeJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String readBody(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = in.read(buf)) >= 0) {
            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
