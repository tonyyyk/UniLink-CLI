package server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Wires up and starts the embedded HTTP server on the given port.
 * Registers all API route handlers and the static file handler for the web UI.
 *
 * HttpServer routes by longest-prefix match, so:
 *   /api/auth    matches /api/auth/login, /api/auth/register, etc.
 *   /            catches everything else (static files)
 */
public class ApiServer {

    private final HttpServer server;

    public ApiServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API routes
        server.createContext("/api/auth",          new AuthHandler());
        server.createContext("/api/profile",       new ProfileHandler());
        server.createContext("/api/match",         new MatchHandler());
        server.createContext("/api/messages",      new MessageHandler());
        server.createContext("/api/notifications", new NotifyHandler());
        server.createContext("/api/groups/chat",   new GroupMessageHandler());
        server.createContext("/api/groups",        new GroupHandler());
        server.createContext("/api/admin",         new AdminHandler());
        server.createContext("/api/reports",       new ReportHandler());
        server.createContext("/api/settings",      new SettingsHandler());

        // Static file handler — serves web/ directory
        server.createContext("/", new StaticFileHandler("web"));

        // Thread pool for concurrent request handling
        server.setExecutor(Executors.newFixedThreadPool(10));
    }

    public void start() {
        server.start();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   UniLink Web UI is running!             ║");
        System.out.println("║   Open: http://localhost:8080            ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}
