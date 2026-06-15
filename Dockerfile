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

# Railway injects PORT env var — this is the port the WebSocket server listens on.
# The value Railway assigns is typically in the range 1024-65535.
# We declare a default so the image works locally too.
ENV PORT=8080

# Expose the WebSocket port (Railway overrides this at runtime via PORT env var)
EXPOSE 8080

# Run the chat server
CMD ["java", "ChatServer"]
