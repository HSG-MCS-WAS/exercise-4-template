#!/bin/bash
# Start a lightweight VNC server for Swing/AWT GUI apps
export DISPLAY=:1

vncserver :1 -geometry 1280x720 -depth 24 -SecurityTypes None 2>/dev/null
sleep 1

DISPLAY=:1 fluxbox &>/dev/null &

websockify --web /usr/share/novnc 6080 localhost:5901 &>/dev/null &
sleep 1

echo "VNC desktop available at http://localhost:6080"
