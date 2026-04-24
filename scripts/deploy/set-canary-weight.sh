#!/usr/bin/env bash
set -euo pipefail

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

target="${1:-all}"
weight="${2:-}"

if [[ -z "$weight" ]]; then
  echo "Canary weight is required." >&2
  exit 1
fi

if ! [[ "$weight" =~ ^[0-9]+$ ]] || (( weight < 0 || weight > 100 )); then
  echo "Canary weight must be an integer between 0 and 100." >&2
  exit 1
fi

annotate_one() {
  local ingress_name="$1"
  kubectl annotate ingress "$ingress_name" -n "$K8S_NAMESPACE" nginx.ingress.kubernetes.io/canary-weight="$weight" --overwrite
}

case "$target" in
  all)
    annotate_one "zigzag-backend-canary"
    annotate_one "alphashopper-web-canary"
    ;;
  backend)
    annotate_one "zigzag-backend-canary"
    ;;
  frontend)
    annotate_one "alphashopper-web-canary"
    ;;
  *)
    echo "Unknown canary target: $target" >&2
    exit 1
    ;;
esac
