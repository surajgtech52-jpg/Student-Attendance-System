import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.Properties;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;

public class SimpleLoginServer {
    private static final String WEB_ROOT = "d:/project/src";
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASS;

    public static void main(String[] args) throws Exception {
        // Load DB config
        try (FileInputStream fis = new FileInputStream("d:/project/config.properties")) {
            Properties cfg = new Properties();
            cfg.load(fis);
            DB_URL = cfg.getProperty("db.url");
            DB_USER = cfg.getProperty("db.user");
            DB_PASS = cfg.getProperty("db.pass");
        } catch (Exception e) {
            System.err.println("Failed to load DB config: " + e.getMessage());
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new StaticHandler());
    server.createContext("/save", new SaveHandler());
    server.createContext("/bulk", new BulkHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:8000/");
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            if (path.equals("/")) path = "/login teacher.html";
            File f = new File(WEB_ROOT + path);
            if (!f.exists() || f.isDirectory()) {
                String notFound = "404 - Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }
            String mime = Files.probeContentType(Paths.get(f.getAbsolutePath()));
            if (mime == null) mime = "application/octet-stream";
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", mime + "; charset=utf-8");
            byte[] bytes = Files.readAllBytes(f.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            // read body
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            String id = null;
            String password = null;
            for (String part : body.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    String key = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String val = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if ("id".equals(key)) id = val;
                    if ("password".equals(key)) password = val;
                }
            }

            if (id == null || password == null || id.isBlank()) {
                String json = "{\"success\":false,\"message\":\"Missing id or password\"}";
                sendJson(exchange, 400, json);
                return;
            }

            // hash password (SHA-256)
            String hashed;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                hashed = sb.toString();
            } catch (Exception e) {
                String json = "{\"success\":false,\"message\":\"Hash error\"}";
                sendJson(exchange, 500, json);
                return;
            }

            // insert into DB
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String sql = "INSERT INTO ab (ID, PASSWORD, DateTime) VALUES (?, ?, ?)" +
                             " ON DUPLICATE KEY UPDATE PASSWORD = VALUES(PASSWORD), DateTime = VALUES(DateTime)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, id);
                    ps.setString(2, hashed);
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    int r = ps.executeUpdate();
                    String json = "{\"success\":true,\"rows\":" + r + "}";
                    sendJson(exchange, 200, json);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String json = "{\"success\":false,\"message\":\"DB error: " + e.getMessage().replace("\"","\\\"") + "\"}";
                sendJson(exchange, 500, json);
                return;
            }
        }

        private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", "application/json; charset=utf-8");
            byte[] b = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, b.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static class BulkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Expecting a JSON array: [{"id":"...","password":"..."}, ...]
            java.util.List<java.util.Map<String,String>> entries = new java.util.ArrayList<>();
            try {
                // Minimal JSON parsing without external libs
                String trimmed = body.trim();
                if (!trimmed.startsWith("[")) throw new IllegalArgumentException("Expected JSON array");
                // Very simple parsing: split objects by '},{'
                String inner = trimmed.substring(1, trimmed.length()-1).trim();
                if (inner.isEmpty()) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Empty array\"}");
                    return;
                }
                String[] objs = inner.split("\\},\\s*\\{");
                for (String o : objs) {
                    String obj = o;
                    if (!obj.startsWith("{")) obj = "{" + obj;
                    if (!obj.endsWith("}")) obj = obj + "}";
                    java.util.Map<String,String> map = new java.util.HashMap<>();
                    // extract simple string fields
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(id|password|role)\"\s*:\s*\"(.*?)\"").matcher(obj);
                    while (m.find()) {
                        map.put(m.group(1), m.group(2));
                    }
                    if (map.containsKey("id") && map.containsKey("password")) entries.add(map);
                }
            } catch (Exception e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid JSON format\"}");
                return;
            }

            if (entries.isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"No valid entries\"}");
                return;
            }

            int inserted = 0;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String sql = "INSERT INTO ab (ID, PASSWORD, DateTime) VALUES (?, ?, ?)" +
                             " ON DUPLICATE KEY UPDATE PASSWORD = VALUES(PASSWORD), DateTime = VALUES(DateTime)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (java.util.Map<String,String> m : entries) {
                        String id = m.get("id");
                        String pw = m.get("password");
                        String hashed;
                        try {
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] hash = digest.digest(pw.getBytes(StandardCharsets.UTF_8));
                            StringBuilder sb = new StringBuilder();
                            for (byte b : hash) sb.append(String.format("%02x", b));
                            hashed = sb.toString();
                        } catch (Exception ex) {
                            continue;
                        }
                        ps.setString(1, id);
                        ps.setString(2, hashed);
                        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        inserted += ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"DB error\"}");
                return;
            }

            sendJson(exchange, 200, "{\"success\":true,\"inserted\":" + inserted + "}");
        }

        private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", "application/json; charset=utf-8");
            byte[] b = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, b.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(b);
            }
        }
    }
}
