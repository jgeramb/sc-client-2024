#!/bin/sh
cd "$(dirname "$0")" || exit
java \
  -Xmx1280M \
  -Xms1280M \
  -XX:+UseSerialGC \
  -XX:-UseParallelGC \
  -XX:NewRatio=1 \
  -jar teamgruen-player.jar \
  --batch-mode \
  --play-style weighted \
  --debug \
  "$@"