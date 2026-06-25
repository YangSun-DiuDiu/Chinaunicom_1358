#!/bin/bash
PID=$(ps aux | grep "ruoyi-admin.jar" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
  kill $PID
  echo "Backend stopped (PID: $PID)"
else
  echo "No running backend found"
fi
