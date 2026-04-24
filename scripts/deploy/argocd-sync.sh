#!/usr/bin/env bash
set -euo pipefail

: "${ARGOCD_SERVER:?ARGOCD_SERVER is required}"
: "${ARGOCD_AUTH_TOKEN:?ARGOCD_AUTH_TOKEN is required}"

environment="${1:?environment is required}"
target="${2:-all}"
timeout_seconds="${ARGOCD_SYNC_TIMEOUT_SECONDS:-900}"

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

sync_and_wait() {
  local app_name="$1"
  argocd app sync "$app_name" --apply-out-of-sync-only --timeout "$timeout_seconds" "${argocd_flags[@]}"
  argocd app wait "$app_name" --sync --health --operation --timeout "$timeout_seconds" "${argocd_flags[@]}"
}

case "$environment" in
  production)
    case "$target" in
      all)
        sync_and_wait "alphashopper-production-root"
        sync_and_wait "alphashopper-production-core"
        sync_and_wait "alphashopper-production-monitoring"
        sync_and_wait "alphashopper-production-ingress-monitoring"
        ;;
      root)
        sync_and_wait "alphashopper-production-root"
        ;;
      core)
        sync_and_wait "alphashopper-production-core"
        ;;
      monitoring)
        sync_and_wait "alphashopper-production-monitoring"
        ;;
      ingress-monitoring)
        sync_and_wait "alphashopper-production-ingress-monitoring"
        ;;
      *)
        echo "Unknown target: $target" >&2
        exit 1
        ;;
    esac
    ;;
  staging)
    case "$target" in
      all)
        sync_and_wait "alphashopper-staging-root"
        sync_and_wait "alphashopper-staging-core"
        sync_and_wait "alphashopper-staging-monitoring"
        sync_and_wait "alphashopper-staging-ingress-monitoring"
        ;;
      root)
        sync_and_wait "alphashopper-staging-root"
        ;;
      core)
        sync_and_wait "alphashopper-staging-core"
        ;;
      monitoring)
        sync_and_wait "alphashopper-staging-monitoring"
        ;;
      ingress-monitoring)
        sync_and_wait "alphashopper-staging-ingress-monitoring"
        ;;
      *)
        echo "Unknown target: $target" >&2
        exit 1
        ;;
    esac
    ;;
  *)
    echo "Unknown environment: $environment" >&2
    exit 1
    ;;
esac
