#!/usr/bin/env bash
# Run a JaCaMo Gradle task with VNC support inside the Dev Container.
# Usage: ./run.sh warmup
#        ./run.sh task1
#        ./run.sh task2
#        ./run.sh task3

TASK="${1:-warmup}"

# Ensure VNC is running (idempotent)
if ! pgrep -x Xtigervnc > /dev/null 2>&1 && ! pgrep -x Xvnc > /dev/null 2>&1; then
    echo "Starting VNC server..."
    export DISPLAY=:1
    vncserver :1 -geometry 1280x720 -depth 24 -SecurityTypes None 2>/dev/null
    sleep 1
    DISPLAY=:1 fluxbox &>/dev/null &
    websockify --web /usr/share/novnc 6080 localhost:5901 &>/dev/null &
    sleep 1
    echo "VNC desktop available at http://localhost:6080"
fi

export DISPLAY=:1
exec ./gradlew --no-daemon "$TASK"
