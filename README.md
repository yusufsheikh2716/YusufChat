# 💬 YusufChat — Real-Time Chat Application

> **Connect Instantly** — A production-ready, cross-platform real-time chat app built with Java (Socket + WebSocket) and a stunning web frontend.
>
> **Made by Mohd Yusuf | BBD NIIT Lucknow**

---

## ✨ Features

| Feature | Web | Desktop |
|---|---|---|
| Real-time messaging | ✅ | ✅ |
| Multiple simultaneous users | ✅ | ✅ |
| Username / join flow | ✅ | ✅ |
| Online users list | ✅ | ✅ |
| Join / Leave notifications | ✅ | ✅ |
| Message timestamps | ✅ | ✅ |
| Typing indicator | ✅ | ✅ |
| Auto-scroll to latest | ✅ | ✅ |
| Dark theme (purple/indigo) | ✅ | ✅ |
| Sound on new message | ✅ | ✅ |
| Unread message count | ✅ | — |
| Auto-reconnect | ✅ | ✅ |
| Mobile responsive | ✅ | — |
| Works on Chrome/Firefox/Safari/Edge | ✅ | — |
| Cross-platform JAR | — | ✅ |

---

## 📁 File Structure

```
YusufChat/
├── server/
│   └── ChatServer.java       ← Main server (TCP + WebSocket)
├── desktop/
│   ├── ChatClient.java       ← Network layer (TCP socket)
│   └── ChatGUI.java          ← Swing GUI
├── web/
│   └── index.html            ← Complete web app (single file)
└── README.md
```

---

## 🖥️ Ports

| Port | Protocol | Used By |
|------|----------|---------|
| `9090` | Raw TCP | Java Desktop clients |
| `9091` | WebSocket | Web browser clients |

---

## 🚀 Quick Start

### Prerequisites

- **Java 11+** (JDK, not just JRE)
- A modern web browser (for web client)
- No external libraries — pure Java standard library!

Check your Java version:
```sh
java -version
```

---

## ⚙️ 1. Compile & Run the Server

### macOS / Linux

```bash
cd YusufChat/server
javac ChatServer.java
java ChatServer
```

### Windows (Command Prompt)

```bat
cd YusufChat\server
javac ChatServer.java
java ChatServer
```

You should see:
```
╔══════════════════════════════════════╗
║         YusufChat Server             ║
║   Made by Mohd Yusuf | BBD NIIT      ║
╠══════════════════════════════════════╣
║  TCP  (Desktop)  → port 9090         ║
║  WebSocket (Web) → port 9091         ║
╚══════════════════════════════════════╝
[SERVER] Both servers running. Waiting for connections...
```

---

## 🌐 2. Open the Web App

1. Start the server (step above)
2. Open `web/index.html` in your browser
   - **Chrome/Edge**: double-click the file, or drag into browser
   - **Or serve locally**: `npx serve web/` then open `http://localhost:3000`
3. Enter your name → `localhost` → port `9091` → **Join Chat**

The web app works on:
- 💻 Chrome, Firefox, Safari, Edge (desktop)
- 📱 Android Chrome, iOS Safari (mobile)

---

## 🖥️ 3. Run the Desktop App (Java Swing)

### Compile

```bash
cd YusufChat/desktop
javac ChatClient.java ChatGUI.java
```

### Run

```bash
java ChatGUI
```

### Build a standalone JAR

```bash
# From the desktop/ directory
jar cfe YusufChat-Desktop.jar ChatGUI ChatClient.class ChatGUI*.class
java -jar YusufChat-Desktop.jar
```

A login dialog will appear. Enter your name, server host, and port (`9090`).

---

## 🌍 Connecting Multiple Devices on the Same Network

1. Find your server machine's **local IP**:
   - **macOS**: `ifconfig | grep "inet " | grep -v 127`
   - **Linux**: `hostname -I`
   - **Windows**: `ipconfig` → look for IPv4 Address

2. Web clients: enter that IP in the **Server Host** field
3. Desktop clients: enter that IP in the **Server Host** field

---

## ☁️ Deploy to the Cloud (Free Hosting)

### Option A: Railway

```bash
# Install Railway CLI
npm install -g @railway/cli
railway login

# From project root
railway init
railway up
```

Set environment variable `PORT=9091` and update the Java server to read:
```java
int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9091"));
```

### Option B: Render

1. Push your code to GitHub
2. Go to [render.com](https://render.com) → **New Web Service**
3. Select your repo
4. **Build Command**: `javac server/ChatServer.java`
5. **Start Command**: `java -cp server ChatServer`
6. **Environment**: Add `PORT=9091`

### Option C: Self-hosted VPS (DigitalOcean, AWS, etc.)

```bash
# On the VPS
scp -r YusufChat/ user@your-vps-ip:~/
ssh user@your-vps-ip
cd ~/YusufChat/server
javac ChatServer.java
# Run in background
nohup java ChatServer > chat.log 2>&1 &
```

Open firewall ports 9090 and 9091 in your VPS dashboard.

---

## 🔧 Configuration

Edit these constants at the top of `ChatServer.java`:

```java
static final int TCP_PORT    = 9090;  // Desktop client port
static final int WS_PORT     = 9091;  // Web client port
static final int MAX_CLIENTS = 200;   // Max simultaneous users
```

---

## 🔒 Security Notes

For production deployment, consider:
- Running behind an **Nginx reverse proxy** with TLS (`wss://`)
- Adding **rate limiting** on the server
- Implementing **authentication** (JWT tokens)
- Sanitizing all message content on the server side

---

## 📱 Mobile Web

The web app is fully responsive and tested on:
- Android Chrome 90+
- iOS Safari 14+
- Firefox Mobile

No app install required — just open the URL in a mobile browser.

---

## 🏗️ Architecture

```
┌──────────────────┐      WebSocket (port 9091)     ┌──────────────────┐
│   Web Browser    │◄──────────────────────────────►│                  │
│   (index.html)   │                                 │  ChatServer.java │
└──────────────────┘                                 │                  │
                                                     │  (Broadcasts to  │
┌──────────────────┐      Raw TCP (port 9090)        │   all clients)   │
│  Java Desktop    │◄──────────────────────────────►│                  │
│  (ChatGUI.java)  │                                 └──────────────────┘
└──────────────────┘
```

The server uses a **shared broadcast model**: every message received from any client (TCP or WebSocket) is forwarded to **all** connected clients on both protocols simultaneously.

---

## 📜 JSON Protocol

All messages are JSON strings (one per line for TCP, one per WebSocket frame for WS):

| Direction | Type | Fields |
|-----------|------|--------|
| Client → Server | `join` | `type`, `username` |
| Client → Server | `message` | `type`, `username`, `text` |
| Client → Server | `typing` | `type`, `username` |
| Client → Server | `stopTyping` | `type`, `username` |
| Server → Client | `message` | `type`, `username`, `text`, `time` |
| Server → Client | `join` | `type`, `username`, `text`, `time` |
| Server → Client | `leave` | `type`, `username`, `text`, `time` |
| Server → Client | `welcome` | `type`, `username`, `text`, `time` |
| Server → Client | `userList` | `type`, `users[]` |
| Server → Client | `typing` | `type`, `username` |
| Server → Client | `stopTyping` | `type`, `username` |

---

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| `Connection refused` | Make sure server is running; check host/port |
| WebSocket blocked | Some browsers block `ws://` on `https://` pages. Use `http://` to serve the HTML or use `wss://` with TLS |
| `javac: command not found` | Install JDK (not just JRE). On macOS: `brew install openjdk` |
| Port already in use | Change port in `ChatServer.java` and update web/desktop accordingly |
| No sound on message | Browsers require a user gesture before audio. Click the Join button and sound will work |
| Desktop app blurry on Mac | Add `-Dapple.awt.application.name=YusufChat` JVM flag |

---

## 👨‍💻 Author

**Mohd Yusuf**  
BBD NIIT Lucknow  

---

*Made with ❤️ using pure Java — no frameworks, no dependencies.*
