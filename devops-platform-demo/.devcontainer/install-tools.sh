#!/usr/bin/env bash
set -euo pipefail

detect_arch() {
  case "$(uname -m)" in
    x86_64)
      ARCH=amd64
      TRIVY_ARCH=64bit
      KYVERNO_ARCH=x86_64
      ;;
    aarch64 | arm64)
      ARCH=arm64
      TRIVY_ARCH=ARM64
      KYVERNO_ARCH=arm64
      ;;
    *)
      printf '❌ unsupported architecture: %s\n' "$(uname -m)" >&2
      return 1
      ;;
  esac
}

fetch() {
  curl -fsSL --retry 3 -o "$2" "$1"
}

verify_sha256() {
  local expected actual
  expected=$(awk -v name="$3" '
    {
      file = $NF
      sub(/^.*\//, "", file)
      sub(/^\*/, "", file)
    }
    file == name { print $1; exit }
  ' "$2")
  if [ -z "$expected" ]; then
    printf '❌ no checksum entry for %s\n' "$3" >&2
    return 1
  fi

  actual=$(sha256sum "$1" | awk '{print $1}')
  if [ "$expected" != "$actual" ]; then
    printf '❌ checksum mismatch for %s (expected %s, got %s)\n' "$3" "$expected" "$actual" >&2
    return 1
  fi
  printf '✅ sha256 ok: %s\n' "$3"
}

install_k3d() {
  local asset="k3d-linux-$ARCH"
  local base="https://github.com/k3d-io/k3d/releases/download/v$K3D_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/checksums.txt" "$WORK/k3d-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/k3d-checksums.txt" "$asset"
  install -m 0755 "$WORK/$asset" "$BIN_DIR/k3d"
}

install_trivy() {
  local asset="trivy_${TRIVY_VERSION}_Linux-$TRIVY_ARCH.tar.gz"
  local base="https://github.com/aquasecurity/trivy/releases/download/v$TRIVY_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/trivy_${TRIVY_VERSION}_checksums.txt" "$WORK/trivy-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/trivy-checksums.txt" "$asset"
  tar -xzf "$WORK/$asset" -C "$WORK" trivy
  install -m 0755 "$WORK/trivy" "$BIN_DIR/trivy"
}

install_kyverno() {
  local asset="kyverno-cli_v${KYVERNO_VERSION}_linux_$KYVERNO_ARCH.tar.gz"
  local base="https://github.com/kyverno/kyverno/releases/download/v$KYVERNO_VERSION"
  fetch "$base/$asset" "$WORK/$asset"
  fetch "$base/checksums.txt" "$WORK/kyverno-checksums.txt"
  verify_sha256 "$WORK/$asset" "$WORK/kyverno-checksums.txt" "$asset"
  tar -xzf "$WORK/$asset" -C "$WORK" kyverno
  install -m 0755 "$WORK/kyverno" "$BIN_DIR/kyverno"
}

main() {
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  # shellcheck source=/dev/null
  . "${VERSIONS_FILE:-$script_dir/versions.env}"
  BIN_DIR="${BIN_DIR:-/usr/local/bin}"
  WORK="$(mktemp -d)"
  trap 'rm -rf "$WORK"' EXIT

  detect_arch
  mkdir -p "$BIN_DIR"
  install_k3d
  install_trivy
  install_kyverno
}

if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  main "$@"
fi
