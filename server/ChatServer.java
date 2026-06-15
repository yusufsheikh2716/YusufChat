/**
 * YusufChat - ChatServer.java
 * Real-time Multi-Client Chat Server
 * Supports: Raw TCP Sockets (Desktop) + WebSocket (Web Browser)
 *
 * Made by Mohd Yusuf | BBD NIIT Lucknow
 *
 * HOW IT WORKS:
 *  - Port 9090: Raw TCP Socket for Java Desktop clients
 *  - Port 9091: WebSocket server for Web Browser clients
 *  - Both share the same broadcast logic / user list
 *
 * Run:  javac ChatServer.java && java ChatServer
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Base64;
import java.nio.charset.*;

public class ChatServer {

    // ─── Configuration ───────────────────────────────────────────────────────────
    static final int TCP_PORT    = 9090; // Java Desktop clients (internal)
    // Railway injects PORT env var; web clients connect to this
    static final int WS_PORT     = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "9091")
    );
    static final int MAX_CLIENTS = 200;

    // ─── Shared State ─────────────────────────────────────────────────────────────
    /** All connected clients (TCP + WebSocket) */
    static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    static final AtomicInteger clientIdCounter = new AtomicInteger(0);

    static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");

    // ─── Entry Point ──────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         YusufChat Server             ║");
        System.out.println("║   Made by Mohd Yusuf | BBD NIIT      ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  TCP  (Desktop)  → port " + TCP_PORT + "         ║");
        System.out.println("║  WebSocket (Web) → port " + WS_PORT + "         ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Start TCP server thread (Desktop clients — may not be accessible on Railway)
        Thread tcpThread = new Thread(() -> {
            try { startTCPServer(); }
            catch (IOException e) { System.err.println("[TCP] Server error (non-fatal on Railway): " + e.getMessage()); }
        }, "TCP-Server");
        tcpThread.setDaemon(true); // Daemon so it doesn't prevent JVM exit if WS server fails
        tcpThread.start();

        // Start WebSocket server thread
        Thread wsThread = new Thread(() -> {
            try { startWebSocketServer(); }
            catch (IOException e) { System.err.println("[WS] Server error: " + e.getMessage()); }
        }, "WS-Server");
        wsThread.setDaemon(false);
        wsThread.start();

        System.out.println("[SERVER] Both servers running. Waiting for connections...");
    }

    // ─── TCP Server (for Java Desktop clients) ────────────────────────────────────
    static void startTCPServer() throws IOException {
        ServerSocket server = new ServerSocket(TCP_PORT);
        server.setReuseAddress(true);
        System.out.println("[TCP] Listening on port " + TCP_PORT);

        while (true) {
            Socket socket = server.accept();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            String id = "tcp-" + clientIdCounter.incrementAndGet();
            TcpClientHandler handler = new TcpClientHandler(socket, id);
            new Thread(handler, "TCP-" + id).start();
            System.out.println("[TCP] New connection: " + id + " from " + socket.getInetAddress());
        }
    }

    // ─── WebSocket Server (for Browser clients) ───────────────────────────────────
    static void startWebSocketServer() throws IOException {
        ServerSocket server = new ServerSocket(WS_PORT);
        server.setReuseAddress(true);
        System.out.println("[WS] Listening on port " + WS_PORT);

        while (true) {
            Socket socket = server.accept();
            socket.setTcpNoDelay(true);
            String id = "ws-" + clientIdCounter.incrementAndGet();
            WebSocketClientHandler handler = new WebSocketClientHandler(socket, id);
            new Thread(handler, "WS-" + id).start();
            System.out.println("[WS] New connection: " + id + " from " + socket.getInetAddress());
        }
    }

    // ─── Broadcast Helpers ────────────────────────────────────────────────────────

    /** Broadcast a JSON message to every connected client */
    static void broadcast(String json) {
        for (ClientHandler c : clients.values()) {
            try { c.sendMessage(json); }
            catch (Exception e) { /* client will be cleaned up by its own thread */ }
        }
    }

    /** Broadcast an updated user list to all clients */
    static void broadcastUserList() {
        StringBuilder sb = new StringBuilder("{\"type\":\"userList\",\"users\":[");
        boolean first = true;
        for (ClientHandler c : clients.values()) {
            if (c.username != null) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(c.username)).append("\"");
                first = false;
            }
        }
        sb.append("]}");
        broadcast(sb.toString());
    }

    /** Build a chat-message JSON packet */
    static String buildMessage(String type, String username, String text) {
        return String.format(
            "{\"type\":\"%s\",\"username\":\"%s\",\"text\":\"%s\",\"time\":\"%s\"}",
            type,
            escapeJson(username),
            escapeJson(text),
            SDF.format(new Date())
        );
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ─── Abstract Client Handler ──────────────────────────────────────────────────
    abstract static class ClientHandler {
        volatile String username;
        final String clientId;

        ClientHandler(String clientId) {
            this.clientId = clientId;
        }

        abstract void sendMessage(String json) throws Exception;
        abstract void disconnect();

        /** Called when this client sends a parsed message JSON */
        void onMessage(String json) {
            try {
                // Very lightweight JSON parsing — no external libraries
                String type     = extractJson(json, "type");
                String username = extractJson(json, "username");
                String text     = extractJson(json, "text");

                switch (type) {
                    case "join":
                        handleJoin(username);
                        break;
                    case "message":
                        handleChat(text);
                        break;
                    case "typing":
                        handleTyping(username);
                        break;
                    case "stopTyping":
                        handleStopTyping(username);
                        break;
                    default:
                        System.out.println("[UNKNOWN] " + clientId + ": " + json);
                }
            } catch (Exception e) {
                System.err.println("[MSG-ERROR] " + clientId + ": " + e.getMessage());
            }
        }

        void handleJoin(String name) throws Exception {
            if (name == null || name.trim().isEmpty()) return;
            this.username = name.trim().substring(0, Math.min(name.trim().length(), 30));
            clients.put(clientId, this);

            System.out.println("[JOIN] " + username + " (" + clientId + ")");

            // Notify everyone
            broadcast(buildMessage("join", username, username + " joined the chat 🎉"));
            broadcastUserList();

            // Send welcome message privately
            sendMessage(buildMessage("welcome", "YusufChat", "Welcome to YusufChat, " + username + "! 👋"));
        }

        void handleChat(String text) {
            if (username == null || text == null || text.trim().isEmpty()) return;
            String trimmed = text.trim().substring(0, Math.min(text.trim().length(), 2000));
            System.out.println("[MSG] " + username + ": " + trimmed);
            broadcast(buildMessage("message", username, trimmed));
        }

        void handleTyping(String name) {
            if (username == null) return;
            String typingJson = String.format(
                "{\"type\":\"typing\",\"username\":\"%s\"}", escapeJson(username)
            );
            // Broadcast to everyone except self
            for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                if (!e.getKey().equals(clientId)) {
                    try { e.getValue().sendMessage(typingJson); } catch (Exception ex) { /* ignore */ }
                }
            }
        }

        void handleStopTyping(String name) {
            if (username == null) return;
            String stopJson = String.format(
                "{\"type\":\"stopTyping\",\"username\":\"%s\"}", escapeJson(username)
            );
            for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                if (!e.getKey().equals(clientId)) {
                    try { e.getValue().sendMessage(stopJson); } catch (Exception ex) { /* ignore */ }
                }
            }
        }

        void onDisconnect() {
            clients.remove(clientId);
            if (username != null) {
                System.out.println("[LEAVE] " + username + " (" + clientId + ")");
                broadcast(buildMessage("leave", username, username + " left the chat"));
                broadcastUserList();
            }
        }

        /** Tiny JSON value extractor — no dependencies */
        String extractJson(String json, String key) {
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
    }

    // ─── TCP Client Handler (Java Desktop) ────────────────────────────────────────
    static class TcpClientHandler extends ClientHandler implements Runnable {
        final Socket socket;
        PrintWriter out;

        TcpClientHandler(Socket socket, String id) {
            super(id);
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter pw = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                this.out = pw;

                String line;
                while ((line = in.readLine()) != null) {
                    onMessage(line.trim());
                }
            } catch (IOException e) {
                // Normal when client disconnects
            } finally {
                disconnect();
                onDisconnect();
            }
        }

        @Override
        public synchronized void sendMessage(String json) {
            if (out != null) {
                out.println(json);
            }
        }

        @Override
        public void disconnect() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ─── WebSocket Client Handler (Browser) ───────────────────────────────────────
    static class WebSocketClientHandler extends ClientHandler implements Runnable {
        final Socket socket;
        InputStream in;
        OutputStream out;
        volatile boolean handshakeDone = false;

        // WebSocket frame opcodes
        static final int OPCODE_TEXT  = 0x1;
        static final int OPCODE_CLOSE = 0x8;
        static final int OPCODE_PING  = 0x9;
        static final int OPCODE_PONG  = 0xA;

        WebSocketClientHandler(Socket socket, String id) {
            super(id);
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = socket.getInputStream();
                out = socket.getOutputStream();

                // 1. Perform HTTP Upgrade handshake
                if (!doHandshake()) {
                    disconnect();
                    return;
                }
                handshakeDone = true;
                System.out.println("[WS] Handshake complete: " + clientId);

                // 2. Read WebSocket frames
                while (!socket.isClosed()) {
                    String msg = readFrame();
                    if (msg == null) break; // connection closed
                    onMessage(msg);
                }
            } catch (IOException e) {
                // Normal on disconnect
            } finally {
                disconnect();
                onDisconnect();
            }
        }

        /** Perform the WebSocket HTTP upgrade handshake */
        boolean doHandshake() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Map<String, String> headers = new HashMap<>();
            String requestLine = reader.readLine();
            if (requestLine == null) return false;

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(),
                                line.substring(colon + 1).trim());
                }
            }

            String key = headers.get("sec-websocket-key");
            if (key == null) {
                // Respond to plain HTTP requests with a simple message
                String response =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Connection: close\r\n\r\n" +
                    "YusufChat WebSocket Server - Connect via WebSocket on port " + WS_PORT;
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return false;
            }

            // Compute the accept key (RFC 6455)
            byte[] acceptBytes;
            try {
                acceptBytes = MessageDigest.getInstance("SHA-1")
                    .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                    .getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("SHA-1 not available", e);
            }
            String acceptKey = Base64.getEncoder().encodeToString(acceptBytes);

            String origin = headers.getOrDefault("origin", "*");
            // Allow known origins; fall back to * for non-browser clients
            String allowedOrigin;
            if (origin.equals("https://yusufchat.netlify.app") ||
                origin.equals("http://localhost:3000") ||
                origin.startsWith("http://localhost:") ||
                origin.startsWith("http://127.0.0.1:")) {
                allowedOrigin = origin;
            } else {
                allowedOrigin = "*";
            }

            String response =
                "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "Access-Control-Allow-Origin: " + allowedOrigin + "\r\n\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return true;
        }

        /** Read one WebSocket text frame (blocking) */
        String readFrame() throws IOException {
            int b0 = in.read();
            if (b0 == -1) return null;
            int b1 = in.read();
            if (b1 == -1) return null;

            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long payloadLen = b1 & 0x7F;

            if (payloadLen == 126) {
                payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else if (payloadLen == 127) {
                payloadLen = 0;
                for (int i = 0; i < 8; i++) payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
            }

            if (payloadLen > 1_000_000) {
                // Too large — close
                sendCloseFrame();
                return null;
            }

            byte[] mask = null;
            if (masked) {
                mask = new byte[4];
                for (int i = 0; i < 4; i++) mask[i] = (byte) in.read();
            }

            byte[] payload = new byte[(int) payloadLen];
            int read = 0;
            while (read < payload.length) {
                int r = in.read(payload, read, payload.length - read);
                if (r == -1) return null;
                read += r;
            }

            if (masked && mask != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i % 4];
                }
            }

            if (opcode == OPCODE_CLOSE) {
                sendCloseFrame();
                return null;
            }
            if (opcode == OPCODE_PING) {
                sendPong(payload);
                return readFrame(); // read next frame
            }
            if (opcode == OPCODE_TEXT) {
                return new String(payload, StandardCharsets.UTF_8);
            }

            return null; // unsupported opcode
        }

        void sendCloseFrame() {
            try {
                out.write(new byte[]{(byte) 0x88, 0x00});
                out.flush();
            } catch (IOException ignored) {}
        }

        void sendPong(byte[] payload) {
            try {
                out.write((byte) 0x8A); // FIN + PONG
                out.write((byte) payload.length);
                out.write(payload);
                out.flush();
            } catch (IOException ignored) {}
        }

        /** Send a WebSocket text frame */
        @Override
        public synchronized void sendMessage(String json) throws Exception {
            if (!handshakeDone || socket.isClosed()) return;
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            frame.write(0x81); // FIN + TEXT opcode

            if (data.length <= 125) {
                frame.write(data.length);
            } else if (data.length <= 65535) {
                frame.write(126);
                frame.write((data.length >> 8) & 0xFF);
                frame.write(data.length & 0xFF);
            } else {
                frame.write(127);
                for (int i = 7; i >= 0; i--) {
                    frame.write((data.length >> (i * 8)) & 0xFF);
                }
            }

            frame.write(data);
            out.write(frame.toByteArray());
            out.flush();
        }

        @Override
        public void disconnect() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
