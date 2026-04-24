#!/usr/bin/env bash
set -euo pipefail

: "${ARGOCD_SERVER:?ARGOCD_SERVER is required}"
: "${ARGOCD_AUTH_TOKEN:?ARGOCD_AUTH_TOKEN is required}"

environment="${1:?environment is required}"

argocd_flags=(
  --server "$ARGOCD_SERVER"
  --auth-token "$ARGOCD_AUTH_TOKEN"
)

if [[ "${ARGOCD_GRPC_WEB:-true}" == "true" ]]; then
  argocd_flags+=(--grpc-web)
fi

if [[ "${ARGOCD_INSECURE:-false}" == "true" ]]; then
  argocd_flags+=(--insecure)
fi

apps_for_environment() {
  case "$1" in
    staging)
      printf '%s\n' \
        alphashopper-staging-root \
        alphashopper-staging-core \
        alphashopper-staging-monitoring \
        alphashopper-staging-ingress-monitoring
      ;;
    production)
      printf '%s\n' \
        alphashopper-production-root \
        alphashopper-production-core \
        alphashopper-production-monitoring \
        alphashopper-production-ingress-monitoring
      ;;
    *)
      echo "Unknown environment: $1" >&2
      exit 1
      ;;
  esac
}

while IFS= read -r app_name; do
  echo "== ${app_name} =="
  argocd app get "$app_name" "${argocd_flags[@]}"
  echo
done < <(apps_for_environment "$environment")
