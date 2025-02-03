#!/bin/sh

echo "Start entrypoint"

echo "[server-startup] Starting java application"
exec java -jar /app/bin/app.jar