#!/usr/bin/env bash
set -euo pipefail

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

target="${1:-all}"

delete_one() {
  local deployment_name="$1"
  local service_name="$2"
  local ingress_name="$3"

  kubectl delete ingress "$ingress_name" -n "$K8S_NAMESPACE" --ignore-not-found
  kubectl delete service "$service_name" -n "$K8S_NAMESPACE" --ignore-not-found
  kubectl delete deployment "$deployment_name" -n "$K8S_NAMESPACE" --ignore-not-found
}

case "$target" in
  all)
    delete_one "zigzag-backend-canary" "zigzag-backend-canary" "zigzag-backend-canary"
    delete_one "alphashopper-web-canary" "alphashopper-web-canary" "alphashopper-web-canary"
    ;;
  backend)
    delete_one "zigzag-backend-canary" "zigzag-backend-canary" "zigzag-backend-canary"
    ;;
  frontend)
    delete_one "alphashopper-web-canary" "alphashopper-web-canary" "alphashopper-web-canary"
    ;;
  *)
    echo "Unknown canary target: $target" >&2
    exit 1
    ;;
esac
