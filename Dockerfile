FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    tigervnc-standalone-server \
    novnc \
    websockify \
    fluxbox \
    nodejs \
    npm \
    libxtst6 \
    libxi6 \
    libxrender1 \
    libfontconfig1 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN chmod +x gradlew start-vnc.sh run.sh \
    && ./gradlew --no-daemon dependencies || true

ENV DISPLAY=:1
