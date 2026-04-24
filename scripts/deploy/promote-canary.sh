#!/usr/bin/env bash
set -euo pipefail

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

target="${1:-all}"

promote_one() {
  local stable_deployment="$1"
  local stable_container="$2"
  local canary_deployment="$3"

  local image
  image="$(kubectl get deployment "$canary_deployment" -n "$K8S_NAMESPACE" -o "jsonpath={.spec.template.spec.containers[0].image}")"

  if [[ -z "$image" ]]; then
    echo "Could not resolve canary image for $canary_deployment" >&2
    exit 1
  fi

  kubectl set image "deployment/${stable_deployment}" "${stable_container}=${image}" -n "$K8S_NAMESPACE"
  kubectl rollout status "deployment/${stable_deployment}" -n "$K8S_NAMESPACE" --timeout=10m
}

case "$target" in
  all)
    promote_one "zigzag-backend" "backend" "zigzag-backend-canary"
    promote_one "alphashopper-web" "web" "alphashopper-web-canary"
    ;;
  backend)
    promote_one "zigzag-backend" "backend" "zigzag-backend-canary"
    ;;
  frontend)
    promote_one "alphashopper-web" "web" "alphashopper-web-canary"
    ;;
  *)
    echo "Unknown canary target: $target" >&2
    exit 1
    ;;
esac
