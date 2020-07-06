#!/bin/bash
FILE=./application.pid
if [[ -f "$FILE" ]]; then
  kill "$(cat "$FILE")" || true
fi
