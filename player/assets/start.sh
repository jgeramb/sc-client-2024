#!/bin/sh
cd "$(dirname "$0")" || exit
java \
  -XX:+UseZGC \
  -jar teamgruen-player.jar \
  --batch-mode \
  --play-style simple \
  "$@"