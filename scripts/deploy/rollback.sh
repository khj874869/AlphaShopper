#!/usr/bin/env bash
set -euo pipefail

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

rollback_target="${1:-}"
revision="${2:-}"

rollback_one() {
  local deployment_name="$1"

  if [[ -n "$revision" ]]; then
    kubectl rollout undo "deployment/${deployment_name}" -n "$K8S_NAMESPACE" --to-revision="$revision"
  else
    kubectl rollout undo "deployment/${deployment_name}" -n "$K8S_NAMESPACE"
  fi

  kubectl rollout status "deployment/${deployment_name}" -n "$K8S_NAMESPACE" --timeout=10m
}

case "$rollback_target" in
  ""|"all")
    rollback_one "zigzag-backend"
    rollback_one "alphashopper-web"
    ;;
  "backend")
    rollback_one "zigzag-backend"
    ;;
  "frontend")
    rollback_one "alphashopper-web"
    ;;
  *)
    echo "Unknown rollback target: $rollback_target" >&2
    exit 1
    ;;
esac
