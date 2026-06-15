# ─── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY server/ChatServer.java .
RUN javac ChatServer.java

# ─── Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Mohd Yusuf | BBD NIIT Lucknow"
LABEL description="YusufChat Server — Real-time WebSocket + TCP Chat"

WORKDIR /app
COPY --from=builder /app/*.class .

# Render injects PORT env var (default 10000).
# Railway also injects PORT (typically 8080 range).
# The Java server reads PORT via System.getenv("PORT") with fallback to 10000.
ENV PORT=10000

# Expose the port Render will route traffic to.
# Render ignores EXPOSE for routing (uses PORT env var), but it's good documentation.
EXPOSE 10000

# Run the chat server
CMD ["java", "ChatServer"]
