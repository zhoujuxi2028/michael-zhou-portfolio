#!/usr/bin/env bash
# Offline assertions for the qa-newman image (REQ-DVP-013).
# Run: bash tests/test-newman-image.sh [image]   (default: qa-newman-demo:test)
set -euo pipefail

IMAGE="${1:-qa-newman-demo:test}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
passed=0
failed=0

check() { # check <description> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

in_image() { docker run --rm --entrypoint sh "$IMAGE" -c "$1"; }

check "Node matches .nvmrc"        "v$(cat "$ROOT/.nvmrc")" "$(in_image 'node -v')"
check "newman version pinned"      "6.2.3"                  "$(docker run --rm "$IMAGE" --version)"
check "htmlextra installed"        "1.23.1"                 "$(in_image 'node -p "require(\"/opt/newman/node_modules/newman-reporter-htmlextra/package.json\").version" 2>/dev/null')"
check "npm removed from runtime"   "absent"                 "$(in_image 'command -v npm >/dev/null && echo present || echo absent')"
check "no handlebars < 4.7.10"     "0"                      "$(in_image 'find / -path /proc -prune -o -path "*/node_modules/handlebars/package.json" -print 2>/dev/null | xargs -r -n1 node -p "const v=require(process.argv[1]).version.split(\".\").map(Number); +(v[0]<4||(v[0]===4&&(v[1]<7||(v[1]===7&&v[2]<10))))" | grep -c 1 || true')"
check "entrypoint is newman"       '["newman"]'             "$(docker image inspect -f '{{json .Config.Entrypoint}}' "$IMAGE")"
check "workdir is /etc/newman"     "/etc/newman"            "$(docker image inspect -f '{{.Config.WorkingDir}}' "$IMAGE")"
check "reports dir world-writable" "777"                    "$(in_image 'stat -c %a /etc/newman/newman-reports')"

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
