#!/usr/bin/env bash
set -euo pipefail

: "${SHOP_BASE_URL:?SHOP_BASE_URL is required}"
: "${API_BASE_URL:?API_BASE_URL is required}"

shop_base_url="${SHOP_BASE_URL%/}"
api_base_url="${API_BASE_URL%/}"
smoke_header_name="${SMOKE_HEADER_NAME:-}"
smoke_header_value="${SMOKE_HEADER_VALUE:-}"

log() {
  printf '[smoke] %s\n' "$1"
}

fetch() {
  local url="$1"
  local -a curl_args

  curl_args=(
    -fsSL
    --retry 3
    --retry-all-errors
    --connect-timeout 5
    --max-time 20
  )

  if [[ -n "$smoke_header_name" ]]; then
    curl_args+=(-H "${smoke_header_name}: ${smoke_header_value}")
  fi

  curl "${curl_args[@]}" "$url"
}

assert_contains() {
  local response="$1"
  local pattern="$2"
  local label="$3"

  if ! grep -Eq "$pattern" <<<"$response"; then
    printf 'Smoke test failed for %s. Pattern not found: %s\n' "$label" "$pattern" >&2
    exit 1
  fi
}

log "Checking storefront root"
storefront_root="$(fetch "${shop_base_url}/")"
assert_contains "$storefront_root" '<!DOCTYPE html|<html' 'storefront root'

log "Checking storefront health endpoint"
storefront_health="$(fetch "${shop_base_url}/api/health")"
assert_contains "$storefront_health" '"status"[[:space:]]*:[[:space:]]*"UP"' 'storefront health'

log "Checking backend readiness"
backend_readiness="$(fetch "${api_base_url}/actuator/health/readiness")"
assert_contains "$backend_readiness" '"status"[[:space:]]*:[[:space:]]*"UP"' 'backend readiness'

log "Checking backend public product catalog"
backend_products="$(fetch "${api_base_url}/api/products")"
assert_contains "$backend_products" '^[[:space:]]*\[' 'backend products'

log "Checking backend coupons endpoint"
backend_coupons="$(fetch "${api_base_url}/api/coupons")"
assert_contains "$backend_coupons" '^[[:space:]]*\[' 'backend coupons'

log "Checking backend CSRF bootstrap endpoint"
backend_csrf="$(fetch "${api_base_url}/api/auth/csrf")"
assert_contains "$backend_csrf" '"token"[[:space:]]*:' 'backend csrf'

log "Smoke tests passed"
