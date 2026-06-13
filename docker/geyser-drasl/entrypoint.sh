#!/usr/bin/env sh
set -eu

exec java ${JAVA_OPTS:-} -jar /opt/geyser/Geyser.jar --nogui --config /data/config.yml
