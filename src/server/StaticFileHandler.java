package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Files;

/**
 * Serves static files from the web/ directory.
 * Maps "/" to web/index.html, "/style.css" to web/style.css, etc.
 */
public class StaticFileHandler implements HttpHandler {

    private final String webRoot;

    public StaticFileHandler(String webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        // Serve index.html for root or unknown paths
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Security: prevent directory traversal
        if (path.contains("..")) {
            ex.sendResponseHeaders(403, -1);
            return;
        }

        File file = new File(webRoot + path);
        if (!file.exists() || !file.isFile()) {
            // Fall back to index.html for SPA client-side routing
            file = new File(webRoot + "/index.html");
            if (!file.exists()) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
        }

        String mimeType = getMimeType(file.getName());
        byte[] content = Files.readAllBytes(file.toPath());

        ex.getResponseHeaders().set("Content-Type", mimeType);
        ex.sendResponseHeaders(200, content.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(content);
        }
    }

    private String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (filename.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".svg"))  return "image/svg+xml";
        if (filename.endsWith(".png"))  return "image/png";
        if (filename.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
