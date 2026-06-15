/**
 * YusufChat - ChatClient.java
 * Desktop Client — Network Layer (TCP Socket)
 *
 * Made by Mohd Yusuf | BBD NIIT Lucknow
 *
 * Responsibilities:
 *  - Maintain a persistent TCP connection to ChatServer
 *  - Parse incoming JSON packets
 *  - Notify the GUI via the MessageListener interface
 *  - Auto-reconnect on connection loss
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class ChatClient {

    // ─── Production Server (Railway) ──────────────────────────────────────────────
    // TCP: amiable-clarity-production-8f65.up.railway.app:9090  (Java Desktop)
    // WSS: wss://amiable-clarity-production-8f65.up.railway.app (Web Browser)
    public static final String DEFAULT_HOST = "amiable-clarity-production-8f65.up.railway.app";
    public static final int    DEFAULT_PORT = 9090; // TCP port for Java Desktop clients

    // ─── State ────────────────────────────────────────────────────────────────────
    private final String host;
    private final int    port;
    private volatile boolean running = false;
    private Socket       socket;
    private PrintWriter  out;
    private String       username;

    // ─── Listeners ────────────────────────────────────────────────────────────────
    private Consumer<ChatMessage> onMessage;
    private Consumer<List<String>> onUserListUpdate;
    private Consumer<String>       onTypingUpdate;
    private Consumer<String>       onStopTyping;
    private Runnable               onConnected;
    private Consumer<String>       onError;

    // ─── Constructor ──────────────────────────────────────────────────────────────
    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ChatClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    // ─── Listener Setters ─────────────────────────────────────────────────────────
    public void setOnMessage(Consumer<ChatMessage> listener)          { this.onMessage = listener; }
    public void setOnUserListUpdate(Consumer<List<String>> listener)  { this.onUserListUpdate = listener; }
    public void setOnTypingUpdate(Consumer<String> listener)          { this.onTypingUpdate = listener; }
    public void setOnStopTyping(Consumer<String> listener)            { this.onStopTyping = listener; }
    public void setOnConnected(Runnable listener)                     { this.onConnected = listener; }
    public void setOnError(Consumer<String> listener)                 { this.onError = listener; }

    // ─── Connection ───────────────────────────────────────────────────────────────

    /**
     * Connect to the server and join with the given username.
     * Starts a background reader thread.
     */
    public void connect(String username) {
        this.username = username;
        running = true;

        Thread connThread = new Thread(() -> {
            while (running) {
                try {
                    doConnect();
                } catch (Exception e) {
                    if (running) {
                        fireError("Connection lost. Reconnecting in 3s... (" + e.getMessage() + ")");
                        try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
        }, "ChatClient-Connector");
        connThread.setDaemon(true);
        connThread.start();
    }

    private void doConnect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);

        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        if (onConnected != null) onConnected.run();

        // Send join packet
        sendRaw(String.format("{\"type\":\"join\",\"username\":\"%s\"}", escapeJson(username)));

        // Read messages
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );

        String line;
        while ((line = reader.readLine()) != null) {
            processIncoming(line.trim());
        }
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ─── Send ─────────────────────────────────────────────────────────────────────

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        sendRaw(String.format("{\"type\":\"message\",\"username\":\"%s\",\"text\":\"%s\"}",
            escapeJson(username), escapeJson(text.trim())));
    }

    public void sendTyping() {
        sendRaw(String.format("{\"type\":\"typing\",\"username\":\"%s\"}", escapeJson(username)));
    }

    public void sendStopTyping() {
        sendRaw(String.format("{\"type\":\"stopTyping\",\"username\":\"%s\"}", escapeJson(username)));
    }

    private synchronized void sendRaw(String json) {
        if (out != null) out.println(json);
    }

    // ─── Receive ──────────────────────────────────────────────────────────────────

    private void processIncoming(String json) {
        String type = extractJson(json, "type");

        switch (type) {
            case "message":
            case "join":
            case "leave":
            case "welcome": {
                String user = extractJson(json, "username");
                String text = extractJson(json, "text");
                String time = extractJson(json, "time");
                ChatMessage msg = new ChatMessage(type, user, text, time, user.equals(username));
                if (onMessage != null) onMessage.accept(msg);
                break;
            }
            case "userList": {
                List<String> users = extractJsonArray(json, "users");
                if (onUserListUpdate != null) onUserListUpdate.accept(users);
                break;
            }
            case "typing": {
                String user = extractJson(json, "username");
                if (onTypingUpdate != null) onTypingUpdate.accept(user);
                break;
            }
            case "stopTyping": {
                String user = extractJson(json, "username");
                if (onStopTyping != null) onStopTyping.accept(user);
                break;
            }
        }
    }

    private void fireError(String msg) {
        if (onError != null) onError.accept(msg);
    }

    // ─── Tiny JSON Helpers ────────────────────────────────────────────────────────

    static String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        idx += search.length();
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx++);
            if (c == '\\' && idx < json.length()) {
                char next = json.charAt(idx++);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static List<String> extractJsonArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return result;
        start += search.length();
        int end = json.indexOf(']', start);
        if (end == -1) return result;
        String arrayStr = json.substring(start, end);
        // Parse quoted strings
        int i = 0;
        while (i < arrayStr.length()) {
            if (arrayStr.charAt(i) == '"') {
                int j = i + 1;
                StringBuilder sb = new StringBuilder();
                while (j < arrayStr.length() && arrayStr.charAt(j) != '"') {
                    if (arrayStr.charAt(j) == '\\') j++;
                    if (j < arrayStr.length()) sb.append(arrayStr.charAt(j));
                    j++;
                }
                result.add(sb.toString());
                i = j + 1;
            } else {
                i++;
            }
        }
        return result;
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ─── Data Model ───────────────────────────────────────────────────────────────

    public static class ChatMessage {
        public final String  type;
        public final String  username;
        public final String  text;
        public final String  time;
        public final boolean isOwn; // message sent by this client

        public ChatMessage(String type, String username, String text, String time, boolean isOwn) {
            this.type     = type;
            this.username = username;
            this.text     = text;
            this.time     = time;
            this.isOwn    = isOwn;
        }
    }
}
