import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import services.MediSearchService;
import services.PatientFileService;
import services.UserFileService;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import models.Patient;
import models.SearchResult;
import models.User;

/**
 * MediSearch - application entry point and web server layer.
 *
 * Data flow:
 *     HTML / CSS / JS  ->  HTTP endpoints in this file  ->  MediSearchService / UserFileService
 *                                                                  |
 *                                          File Handling (data/patients/*.txt, data/users/users.txt)
 *                                                                  |
 *                                             Aho-Corasick matching engine
 *                                                                  |
 *                                                            Search results
 *
 * Built using ONLY Java standard library HttpServer. No external database or frameworks used.
 *
 * Endpoints:
 *     POST /api/login     -> Authenticate user from data/users/users.txt
 *     POST /api/logout    -> Invalidate session
 *     GET  /api/session   -> Check authentication state
 *     GET  /api/patients  -> List patient records read from data/patients/*.txt
 *     POST /api/search    -> Aho-Corasick multi-keyword search
 *     POST /api/patients  -> Create a new patient .txt file
 *     GET  /*             -> Static frontend files from public/
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final int PORT_ATTEMPTS = 5;
    private static final String PUBLIC_DIRECTORY = "public";

    private static MediSearchService service;
    private static UserFileService userService;
    private static final Map<String, User> activeSessions = new ConcurrentHashMap<String, User>();

    public static void main(String[] args) throws IOException {
        PatientFileService fileService = new PatientFileService();
        if (!fileService.ensureDataDirectoryExists()) {
            System.err.println("Could not create patient data directory: "
                    + fileService.getDataDirectory().getAbsolutePath());
        }

        userService = new UserFileService();
        if (!userService.ensureUsersDirectoryAndFileExists()) {
            System.err.println("Could not create user data directory: "
                    + userService.getUsersDirectory().getAbsolutePath());
        }

        service = new MediSearchService(fileService);
        int initialPatientCount = service.getAllPatients().size();

        HttpServer server = startServer();

        System.out.println();
        System.out.println("=====================================================");
        System.out.println("  MediSearch - A Searchable Patient Repository");
        System.out.println("=====================================================");
        System.out.println("  Server running at : http://localhost:" + server.getAddress().getPort());
        System.out.println("  Patient records   : " + fileService.getDataDirectory().getAbsolutePath());
        System.out.println("  User credentials  : " + userService.getUsersDirectory().getAbsolutePath());
        System.out.println("  Records loaded    : " + initialPatientCount);
        System.out.println("  Search engine     : Aho-Corasick (Trie + failure links + bitmask)");
        System.out.println("  Demo account      : Username: admin | Password: admin123");
        System.out.println("=====================================================");
        System.out.println("  Open the URL above in a browser. Press Ctrl+C to stop.");
        System.out.println();
    }

    private static HttpServer startServer() throws IOException {
        HttpServer server = null;
        IOException lastError = null;

        for (int attempt = 0; attempt < PORT_ATTEMPTS; attempt++) {
            int port = DEFAULT_PORT + attempt;
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (BindException e) {
                lastError = e;
                System.out.println("Port " + port + " is in use, trying " + (port + 1) + "...");
            }
        }

        if (server == null) {
            throw lastError != null ? lastError : new IOException("Could not start server.");
        }

        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/logout", new LogoutHandler());
        server.createContext("/api/session", new SessionHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    // ------------------------------------------------------------- SESSION UTILS

    private static String getSessionToken(HttpExchange exchange) {
        // Check Authorization / X-Session-Token header first
        List<String> authHeaders = exchange.getRequestHeaders().get("X-Session-Token");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            return authHeaders.get(0).trim();
        }

        // Check Cookies
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] pairs = cookieHeader.split(";");
                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=");
                    if (kv.length == 2 && kv[0].equalsIgnoreCase("session_token")) {
                        return kv[1].trim();
                    }
                }
            }
        }
        return null;
    }

    private static User getAuthenticatedUser(HttpExchange exchange) {
        String token = getSessionToken(exchange);
        if (token != null && activeSessions.containsKey(token)) {
            return activeSessions.get(token);
        }
        return null;
    }

    // ---------------------------------------------------------------- API: LOGIN

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed. Use POST.");
                    return;
                }

                Map<String, String> form = parseFormBody(readBody(exchange));
                String username = form.get("username");
                String password = form.get("password");

                if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                    sendError(exchange, 400, "Username and password are required.");
                    return;
                }

                User user = userService.authenticate(username, password);
                if (user == null) {
                    sendError(exchange, 401, "Invalid username or password.");
                    return;
                }

                String token = UUID.randomUUID().toString();
                activeSessions.put(token, user);

                exchange.getResponseHeaders().add("Set-Cookie", "session_token=" + token + "; Path=/; HttpOnly");

                sendJson(exchange, 200, "{"
                        + "\"success\": true, "
                        + "\"sessionToken\": \"" + token + "\", "
                        + "\"user\": " + user.toJson()
                        + "}");
            } catch (Exception e) {
                logFailure("LoginHandler", e);
                sendError(exchange, 500, "Login processing failed.");
            } finally {
                exchange.close();
            }
        }
    }

    // --------------------------------------------------------------- API: LOGOUT

    static class LogoutHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed. Use POST.");
                    return;
                }

                String token = getSessionToken(exchange);
                if (token != null) {
                    activeSessions.remove(token);
                }

                exchange.getResponseHeaders().add("Set-Cookie", "session_token=; Path=/; Max-Age=0; HttpOnly");
                sendJson(exchange, 200, "{\"success\": true, \"message\": \"Logged out successfully.\"}");
            } catch (Exception e) {
                logFailure("LogoutHandler", e);
                sendError(exchange, 500, "Logout processing failed.");
            } finally {
                exchange.close();
            }
        }
    }

    // -------------------------------------------------------------- API: SESSION

    static class SessionHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                User user = getAuthenticatedUser(exchange);
                if (user != null) {
                    sendJson(exchange, 200, "{\"authenticated\": true, \"user\": " + user.toJson() + "}");
                } else {
                    sendJson(exchange, 200, "{\"authenticated\": false}");
                }
            } catch (Exception e) {
                logFailure("SessionHandler", e);
                sendError(exchange, 500, "Session check failed.");
            } finally {
                exchange.close();
            }
        }
    }

    // ------------------------------------------------------------- API: PATIENTS

    static class PatientsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                User currentUser = getAuthenticatedUser(exchange);
                if (currentUser == null) {
                    sendError(exchange, 401, "Authentication required. Please log in.");
                    return;
                }

                String method = exchange.getRequestMethod();
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetAll(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreate(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed. Use GET or POST.");
                }
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                logFailure("PatientsHandler", e);
                sendError(exchange, 500, "Request could not be completed.");
            } finally {
                exchange.close();
            }
        }

        private void handleGetAll(HttpExchange exchange) throws IOException {
            List<Patient> patients = service.getAllPatients();

            StringBuilder json = new StringBuilder();
            json.append("{\"totalPatients\": ").append(patients.size())
                .append(", \"totalRecords\": ").append(service.getTotalRecords())
                .append(", \"patients\": [");

            for (int i = 0; i < patients.size(); i++) {
                if (i > 0) {
                    json.append(", ");
                }
                json.append(patients.get(i).toJson());
            }
            json.append("]}");

            sendJson(exchange, 200, json.toString());
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            Map<String, String> form = parseFormBody(readBody(exchange));

            Patient patient = service.addPatient(
                    form.get("name"),
                    form.get("age"),
                    form.get("gender"),
                    form.get("bloodGroup"),
                    form.get("diagnosis"),
                    form.get("symptoms"),
                    form.get("medications"),
                    form.get("labReports"),
                    form.get("appointments"));

            System.out.println("[MediSearch] Added patient " + patient.getPatientId()
                    + " (" + patient.getName() + ")");

            sendJson(exchange, 200, "{\"success\": true, \"message\": \"Patient "
                    + Patient.escapeJson(patient.getPatientId()) + " saved to data/patients/ as a new file.\", "
                    + "\"patient\": " + patient.toJson() + "}");
        }
    }

    // --------------------------------------------------------------- API: SEARCH

    static class SearchHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                User currentUser = getAuthenticatedUser(exchange);
                if (currentUser == null) {
                    sendError(exchange, 401, "Authentication required. Please log in.");
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed. Use POST.");
                    return;
                }

                Map<String, String> form = parseFormBody(readBody(exchange));
                String rawKeywords = form.get("keywords");

                List<SearchResult> results = service.search(rawKeywords);
                String[] usedKeywords = MediSearchService.sanitizeKeywords(rawKeywords);

                StringBuilder json = new StringBuilder();
                json.append("{\"keywords\": [");
                for (int i = 0; i < usedKeywords.length; i++) {
                    if (i > 0) {
                        json.append(", ");
                    }
                    json.append('"').append(Patient.escapeJson(usedKeywords[i])).append('"');
                }
                json.append("], \"totalMatches\": ").append(results.size())
                    .append(", \"results\": [");

                for (int i = 0; i < results.size(); i++) {
                    if (i > 0) {
                        json.append(", ");
                    }
                    json.append(results.get(i).toJson());
                }
                json.append("]}");

                sendJson(exchange, 200, json.toString());
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                logFailure("SearchHandler", e);
                sendError(exchange, 500, "Search could not be completed.");
            } finally {
                exchange.close();
            }
        }
    }

    // ------------------------------------------------------- STATIC FILE HANDLER

    static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.isEmpty()) {
                    path = "/index.html";
                }

                File publicDirectory = new File(PUBLIC_DIRECTORY).getCanonicalFile();
                File requested = new File(publicDirectory, path).getCanonicalFile();

                String publicRoot = publicDirectory.getPath() + File.separator;
                if (!(requested.getPath() + File.separator).startsWith(publicRoot)
                        || !requested.isFile()) {
                    sendPlainText(exchange, 404, "404 Not Found: " + path);
                    return;
                }

                byte[] content = readAllBytes(requested);
                exchange.getResponseHeaders().set("Content-Type", contentTypeFor(requested.getName()));
                exchange.sendResponseHeaders(200, content.length);

                OutputStream out = exchange.getResponseBody();
                out.write(content);
                out.flush();
            } catch (Exception e) {
                logFailure("StaticFileHandler", e);
                sendPlainText(exchange, 500, "500 Internal Server Error");
            } finally {
                exchange.close();
            }
        }
    }

    // ------------------------------------------------------------------- HELPERS

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> values = new HashMap<String, String>();
        if (body == null || body.isEmpty()) {
            return values;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = decodeUrl(pair.substring(0, equals));
            String value = decodeUrl(pair.substring(equals + 1));
            values.put(key, value);
        }
        return values;
    }

    private static String decodeUrl(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(Charset.forName("UTF-8"));
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.flush();
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String safeMessage = message == null ? "Unexpected error." : message;
        sendJson(exchange, status, "{\"success\": false, \"error\": \""
                + Patient.escapeJson(safeMessage) + "\"}");
    }

    private static void sendPlainText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.flush();
    }

    private static byte[] readAllBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } finally {
            in.close();
        }
    }

    private static String contentTypeFor(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css"))  return "text/css; charset=utf-8";
        if (lower.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".ico"))  return "image/x-icon";
        if (lower.endsWith(".png"))  return "image/png";
        return "text/plain; charset=utf-8";
    }

    private static void logFailure(String where, Exception e) {
        System.err.println("[MediSearch] Error in " + where + ": " + e);
        e.printStackTrace();
    }
}
