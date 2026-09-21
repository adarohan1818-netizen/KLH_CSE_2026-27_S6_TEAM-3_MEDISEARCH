package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import models.Patient;
import repository.PatientRepository;
import algorithms.KMPSearch;
import algorithms.RabinKarpSearch;
import algorithms.AhoCorasick;
import algorithms.EditDistance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class MediSearchServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // API Endpoints
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/search/kmp", new KMPSearchHandler());
        server.createContext("/api/search/rabinkarp", new RabinKarpSearchHandler());
        server.createContext("/api/search/aho", new AhoCorasickSearchHandler());
        server.createContext("/api/search/similar", new SimilarSearchHandler());
        
        // Static Files Handler
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null); // creates a default executor
        System.out.println("MediSearch Server is starting on http://localhost:" + PORT);
        server.start();
    }

    // --- Handlers ---
    
    static class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            setCorsHeaders(t);
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            List<Patient> patients = PatientRepository.getAllPatients();
            String response = buildJsonArray(patients);
            sendJsonResponse(t, response);
        }
    }

    static class KMPSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            setCorsHeaders(t);
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            String query = getQueryParam(t.getRequestURI().getQuery(), "query");
            List<Patient> results = KMPSearch.search(query);
            String response = buildJsonArray(results);
            sendJsonResponse(t, response);
        }
    }

    static class RabinKarpSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            setCorsHeaders(t);
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            String query = getQueryParam(t.getRequestURI().getQuery(), "query");
            List<Patient> results = RabinKarpSearch.search(query);
            String response = buildJsonArray(results);
            sendJsonResponse(t, response);
        }
    }

    static class AhoCorasickSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            setCorsHeaders(t);
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            String keywordsParam = getQueryParam(t.getRequestURI().getQuery(), "keywords");
            String[] keywords = keywordsParam != null ? keywordsParam.split(",") : new String[0];
            List<AhoCorasick.SearchResult> results = AhoCorasick.search(keywords);
            
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                sb.append(results.get(i).toJson());
                if (i < results.size() - 1) sb.append(",");
            }
            sb.append("]");
            
            sendJsonResponse(t, sb.toString());
        }
    }

    static class SimilarSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            setCorsHeaders(t);
            if ("OPTIONS".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(204, -1);
                return;
            }
            String query = getQueryParam(t.getRequestURI().getQuery(), "query");
            // Allow up to max 15 distance for similarity
            List<EditDistance.SimilarityResult> results = EditDistance.findSimilarCases(query, 15);
            
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                sb.append(results.get(i).toJson());
                if (i < results.size() - 1) sb.append(",");
            }
            sb.append("]");
            
            sendJsonResponse(t, sb.toString());
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html";
                else if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                
                t.getResponseHeaders().set("Content-Type", contentType);
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                FileInputStream fs = new FileInputStream(file);
                final byte[] buffer = new byte[0x10000];
                int count = 0;
                while ((count = fs.read(buffer)) >= 0) {
                    os.write(buffer, 0, count);
                }
                fs.close();
                os.close();
            } else {
                String response = "404 (Not Found)\n";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // --- Helper Methods ---

    private static void setCorsHeaders(HttpExchange t) {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private static String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0 && pair.substring(0, idx).equals(param)) {
                return java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    private static String buildJsonArray(List<Patient> patients) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < patients.size(); i++) {
            sb.append(patients.get(i).toJson());
            if (i < patients.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static void sendJsonResponse(HttpExchange t, String response) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        t.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = t.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
