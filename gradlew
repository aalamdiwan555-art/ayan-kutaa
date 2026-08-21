#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=$(sed -n 's/^distributionUrl=.*gradle-\([0-9.]*\)-bin.zip$/\1/p' "$APP_HOME/gradle/wrapper/gradle-wrapper.properties")
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
DIST_DIR=$(find "$GRADLE_HOME" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | head -n 1 || true)

if [ -n "$DIST_DIR" ] && [ -x "$DIST_DIR/bin/gradle" ]; then
  exec "$DIST_DIR/bin/gradle" "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle $GRADLE_VERSION is not installed. Run this project in Gradle/Android Studio or install the wrapper distribution." >&2
exit 1