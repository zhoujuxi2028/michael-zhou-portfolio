#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT

# shellcheck source=/dev/null
. "$HERE/../install-tools.sh"

passed=0
failed=0
check() {
  local desc=$1 want=$2 code=0 out
  shift 2
  out=$("$@" 2>&1) || code=$?
  if [ "$code" = "$want" ]; then
    echo "PASS $desc"
    passed=$((passed + 1))
  else
    echo "FAIL $desc (exit $code, want $want)"
    printf '%s\n' "$out" | sed 's/^/    /'
    failed=$((failed + 1))
  fi
}

printf 'payload\n' > "$T/k3d-linux-amd64"
sha=$(sha256sum "$T/k3d-linux-amd64" | awk '{print $1}')

printf '%s  _dist/k3d-linux-amd64\n%s  _dist/k3d-linux-arm64\n' "$sha" "0000" > "$T/k3d-sums"
check "k3d checksum 路径前缀可匹配" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/k3d-sums" k3d-linux-amd64

printf '%s  k3d-linux-amd64\n' "$sha" > "$T/plain-sums"
check "普通 checksum 条目可匹配" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/plain-sums" k3d-linux-amd64

printf '%s  *k3d-linux-amd64\n' "$sha" > "$T/star-sums"
check "带二进制标记的 checksum 条目可匹配" 0 verify_sha256 "$T/k3d-linux-amd64" "$T/star-sums" k3d-linux-amd64

printf '%s  k3d-linux-amd64\n' "deadbeef" > "$T/bad-sums"
check "checksum 不匹配时失败" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/bad-sums" k3d-linux-amd64

printf '%s  other-asset\n' "$sha" > "$T/missing-sums"
check "找不到对应 checksum 条目时失败" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/missing-sums" k3d-linux-amd64

printf '%s  xk3d-linux-amd64\n' "$sha" > "$T/suffix-sums"
check "仅文件名后缀相同不能匹配" 1 verify_sha256 "$T/k3d-linux-amd64" "$T/suffix-sums" k3d-linux-amd64

if detect_arch; then
  case "$(uname -m)" in
    x86_64)
      [ "$ARCH" = amd64 ] && [ "$TRIVY_ARCH" = 64bit ] && [ "$KYVERNO_ARCH" = x86_64 ] && ok=1 || ok=0
      ;;
    aarch64 | arm64)
      [ "$ARCH" = arm64 ] && [ "$TRIVY_ARCH" = ARM64 ] && [ "$KYVERNO_ARCH" = arm64 ] && ok=1 || ok=0
      ;;
    *) ok=0 ;;
  esac
else
  ok=0
fi
if [ "$ok" = 1 ]; then
  echo "PASS 当前 CPU 架构映射正确"
  passed=$((passed + 1))
else
  echo "FAIL 当前 CPU 架构映射"
  failed=$((failed + 1))
fi

mkdir -p "$T/bin-unsupported"
printf '#!/bin/sh\necho riscv64\n' > "$T/bin-unsupported/uname"
chmod +x "$T/bin-unsupported/uname"
cat > "$T/detect-arch.sh" <<'EOF'
#!/usr/bin/env bash
. "$1"
detect_arch
EOF
check "不支持的 CPU 架构会失败" 1 \
  env PATH="$T/bin-unsupported:$PATH" bash "$T/detect-arch.sh" "$HERE/../install-tools.sh"

echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
