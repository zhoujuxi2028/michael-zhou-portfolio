#!/usr/bin/env bash
# Replay the TDD red/green evidence for given commits.
#   RED:   the commit's new/changed tests run against the PARENT commit's source -> must fail
#   GREEN: the same tests run at the commit itself                               -> must pass
# Usage (from anywhere inside the repo):
#   adas-log-toolchain-lab/scripts/tdd-redgreen.sh <commit>...
#   adas-log-toolchain-lab/scripts/tdd-redgreen.sh $(git log --reverse --format=%h -- adas-log-toolchain-lab/tests)
set -u
REPO=$(git rev-parse --show-toplevel)
P=adas-log-toolchain-lab
WT=$(mktemp -d)/wt
cd "$REPO"

summary() { grep -E '^ +Tests ' | sed 's/^ *//'; }

for c in "$@"; do
  parent=$(git rev-parse "$c^")
  changed=$(git diff --name-only --diff-filter=AM "$parent" "$c" -- "$P/tests" "$P/fixtures" | grep -v __snapshots__)
  tests=$(echo "$changed" | grep '\.test\.ts$' | grep -v "^$P/tests/perf/" | sed "s#^$P/##" | tr '\n' ' ')
  [ -z "$tests" ] && continue

  git worktree add -q --detach "$WT" "$parent"
  ln -s "$REPO/$P/node_modules" "$WT/$P/node_modules" 2>/dev/null || {
    mkdir -p "$WT/$P"; ln -s "$REPO/$P/node_modules" "$WT/$P/node_modules"
    for f in package.json tsconfig.json vitest.config.ts; do git show "$c:$P/$f" > "$WT/$P/$f"; done
  }
  for f in $changed; do mkdir -p "$WT/$(dirname "$f")"; git show "$c:$f" > "$WT/$f"; done

  out=$(cd "$WT/$P" && npx vitest run $tests 2>&1)
  red=$(echo "$out" | summary)
  if [ -z "$red" ] || echo "$red" | grep -q 'no tests'; then
    red="suite fails to load: $(echo "$out" | grep -m1 -oE "(Cannot find module '[^']*'|ENOENT: no such file or directory)")"
  fi

  git -C "$WT" checkout -q --detach -f "$c"
  green=$(cd "$WT/$P" && npx vitest run $tests 2>&1 | summary)
  echo "$(git log -1 --format='%h | %s' "$c") | $tests| RED: $red | GREEN: $green"

  git worktree remove --force "$WT"
done
git worktree prune
