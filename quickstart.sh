#!/usr/bin/env bash
# Copyright 2026 QRun.IO, Inc. Licensed under the Apache License, Version 2.0.
set -euo pipefail

usage() {
  printf 'Usage: bash quickstart.sh [new-directory]\n'
  printf 'Requires JDK 21+, Git, curl, unzip, and a running Docker daemon.\n'
  printf 'Clones the sample (default: qqq-sample), compiles, and opens Next.\n'
  printf 'Inside an existing sample checkout, runs that source. Ctrl+C stops it.\n'
}
if [[ ${1:-} == --help || ${1:-} == -h ]]; then usage; exit 0; fi
if (( $# > 1 )); then usage >&2; exit 1; fi

started=$SECONDS
image=${QQQ_DASHBOARD_IMAGE:-ghcr.io/qrun-io/qqq-frontend-next:0.1.0}
source_ref=${QQQ_QUICKSTART_REF:-quickstart-4.0.0}
app_pid=
pull_pid=
container=

fail() { printf '%s\n' "$*" >&2; exit 1; }
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
command -v docker >/dev/null || fail 'Install and start Docker: https://docs.docker.com/get-started/get-docker/'
java_version=$("$java_command" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [[ ! "$java_version" =~ ^[0-9]+$ ]] || (( java_version < 21 )); then
  fail 'Java 21 or later is required. Check java -version and JAVA_HOME.'
fi
docker info >/dev/null 2>&1 || fail 'Start Docker Desktop (or your Docker daemon), then run this script again.'
for port in 8000 3000; do
  if (echo > "/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1; then
    fail "Port $port is already in use. Stop that application before starting this sample."
  fi
done

cleanup() {
  [[ -z "$app_pid" ]] || kill "$app_pid" 2>/dev/null || true
  [[ -z "$pull_pid" ]] || kill "$pull_pid" 2>/dev/null || true
  [[ -z "$container" ]] || docker rm -f "$container" >/dev/null 2>&1 || true
}
trap cleanup EXIT
trap 'exit 0' INT TERM

printf '[1/4] Preparing Next dashboard and editable application source...\n'
(docker image inspect "$image" >/dev/null 2>&1 || exec docker pull "$image") &
pull_pid=$!
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
if [[ -f "$script_dir/qqq-sample-project/pom.xml" && -x "$script_dir/mvnw" ]]; then
  project=$script_dir
else
  destination=${1:-qqq-sample}
  [[ ! -e "$destination" ]] || fail "Directory already exists: $destination. Choose another directory or run its quickstart.sh."
  git clone --depth 1 --filter=blob:none --sparse --branch "$source_ref" https://github.com/QRun-IO/qqq.git "$destination"
  git -C "$destination" sparse-checkout set qqq-sample-project checkstyle .mvn
  project=$(cd -- "$destination" && pwd)
fi
cd -- "$project"
printf '[2/4] Compiling the sample against published QQQ 4.0.0...\n'
printf 'Build and application log: %s/quickstart.log\n' "$project"
./mvnw -B -q -f qqq-sample-project/pom.xml -Drevision=4.0.0 compile exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleJavalinServer \
  -Dqqq.sample.mockAuthentication=true > quickstart.log 2>&1 &
app_pid=$!
wait "$pull_pid" || fail 'Dashboard download failed. Check the Docker output above.'
pull_pid=
container=$(docker run -d --rm --add-host host.docker.internal:host-gateway \
  -p 127.0.0.1:3000:3000 "$image")
printf '[3/4] Waiting for the application and Next dashboard...\n'
deadline=$((SECONDS + 300))
until curl -fsS --max-time 2 http://127.0.0.1:3000/qqq/v1/metaData/authentication >/dev/null 2>&1 \
  && curl -fsS --max-time 2 http://127.0.0.1:3000/app/person >/dev/null 2>&1; do
  kill -0 "$app_pid" 2>/dev/null || { tail -30 quickstart.log >&2; fail 'Application stopped before it was ready.'; }
  [[ $(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null) == true ]] || fail 'Next dashboard stopped before it was ready.'
  (( SECONDS < deadline )) || fail 'Startup timed out. See quickstart.log; verify ports 8000 and 3000 are free.'
  sleep 1
done
printf '[4/4] Ready in %s seconds: http://localhost:3000/app/person\n' "$((SECONDS - started))"
printf 'Edit Java source in %s/qqq-sample-project/src/main/java.\n' "$project"
printf 'Press Ctrl+C to stop. Run ./quickstart.sh again after editing; sample data resets on restart.\n'
if [[ ${QQQ_NO_BROWSER:-false} != true ]]; then
  if command -v open >/dev/null; then open http://localhost:3000/app/person
  elif command -v xdg-open >/dev/null; then xdg-open http://localhost:3000/app/person >/dev/null 2>&1 || true
  fi
fi
wait "$app_pid"
