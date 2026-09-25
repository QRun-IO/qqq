#!/usr/bin/env bash
# Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0.
set -euo pipefail

usage() {
  printf 'Usage: bash quickstart.sh [new-directory]\n'
  printf 'Requires JDK 21+, Git, curl and unzip.\n'
  printf 'Clones the sample (default: qqq-sample), compiles it, and opens the Next dashboard.\n'
  printf 'Inside an existing sample checkout, runs that source. Ctrl+C stops it.\n'
  printf 'Set QQQ_FRONTEND=material to open the Material Dashboard instead.\n'
}
if [[ ${1:-} == --help || ${1:-} == -h ]]; then usage; exit 0; fi
if (( $# > 1 )); then usage >&2; exit 1; fi

started=$SECONDS
source_ref=${QQQ_QUICKSTART_REF:-quickstart-4.1.0}
qqq_version=${QQQ_QUICKSTART_VERSION:-4.1.0}
frontend=${QQQ_FRONTEND:-next}
app_pid=

fail() { printf '%s\n' "$*" >&2; exit 1; }
case "$frontend" in
  next|material) ;;
  *) fail 'QQQ_FRONTEND must be next or material.' ;;
esac
command -v git >/dev/null || fail 'Install Git: https://git-scm.com/downloads'
java_command=java
javac_command=javac
if [[ -n ${JAVA_HOME:-} ]]; then
  java_command="$JAVA_HOME/bin/java"
  javac_command="$JAVA_HOME/bin/javac"
fi
command -v "$java_command" >/dev/null || fail 'Install JDK 21 and check JAVA_HOME: https://adoptium.net/temurin/releases/?version=21'
command -v "$javac_command" >/dev/null || fail 'A JDK is required, not just a Java runtime. Install JDK 21 and check JAVA_HOME: https://adoptium.net/temurin/releases/?version=21'
command -v curl >/dev/null || fail 'Install curl, then run this script again.'
command -v unzip >/dev/null || fail 'Install unzip for the Maven Wrapper, then run this script again.'
java_version=$("$java_command" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [[ ! "$java_version" =~ ^[0-9]+$ ]] || (( java_version < 21 )); then
  fail 'Java 21 or later is required. Check java -version and JAVA_HOME.'
fi
if (echo > /dev/tcp/127.0.0.1/8000) >/dev/null 2>&1; then
  fail 'Port 8000 is already in use. Stop that application before starting this sample.'
fi

cleanup() {
  [[ -z "$app_pid" ]] || kill "$app_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 0' INT TERM

printf '[1/3] Preparing editable application source...\n'
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
if [[ -f "$script_dir/qqq-sample-project/pom.xml" && -x "$script_dir/mvnw" ]]; then
  project=$script_dir
else
  destination=${1:-qqq-sample}
  [[ ! -e "$destination" ]] || fail "Directory already exists: $destination. Choose another directory or run its quickstart.sh."
  git clone --depth 1 --filter=blob:none --sparse --branch "$source_ref" https://github.com/QRun-IO/qqq.git "$destination"
  git -C "$destination" sparse-checkout set qqq-sample-project checkstyle pmd .mvn
  project=$(cd -- "$destination" && pwd)
fi
cd -- "$project"
printf '[2/3] Compiling the sample against published QQQ %s...\n' "$qqq_version"
printf 'Build and application log: %s/quickstart.log\n' "$project"
./mvnw -B -q -f qqq-sample-project/pom.xml -Drevision="$qqq_version" compile exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleJavalinServer \
  -Dqqq.sample.mockAuthentication=true \
  -Dqqq.javalin.frontend="$frontend" > quickstart.log 2>&1 &
app_pid=$!
printf '[3/3] Waiting for the application and its %s dashboard...\n' "$frontend"
deadline=$((SECONDS + 300))
until curl -fsS --max-time 2 http://127.0.0.1:8000/qqq/v1/metaData/authentication >/dev/null 2>&1 \
  && curl -fsS --max-time 2 http://127.0.0.1:8000/ >/dev/null 2>&1; do
  kill -0 "$app_pid" 2>/dev/null || { tail -30 quickstart.log >&2; fail 'Application stopped before it was ready.'; }
  (( SECONDS < deadline )) || fail 'Startup timed out. See quickstart.log; verify port 8000 is free.'
  sleep 1
done
if [[ $frontend == next ]]; then url=http://localhost:8000/app/person; else url=http://localhost:8000/; fi
printf 'Ready in %s seconds: %s\n' "$((SECONDS - started))" "$url"
printf 'Edit Java source in %s/qqq-sample-project/src/main/java.\n' "$project"
printf 'Press Ctrl+C to stop. Run ./quickstart.sh again after editing; sample data resets on restart.\n'
if [[ ${QQQ_NO_BROWSER:-false} != true ]]; then
  if command -v open >/dev/null; then open "$url"
  elif command -v xdg-open >/dev/null; then xdg-open "$url" >/dev/null 2>&1 || true
  fi
fi
wait "$app_pid"
