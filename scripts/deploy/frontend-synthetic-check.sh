#!/usr/bin/env bash
set -euo pipefail

: "${SHOP_BASE_URL:?SHOP_BASE_URL is required}"

shop_base_url="${SHOP_BASE_URL%/}"
smoke_header_name="${SMOKE_HEADER_NAME:-}"
smoke_header_value="${SMOKE_HEADER_VALUE:-}"

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
    printf 'Frontend synthetic check failed for %s. Pattern not found: %s\n' "$label" "$pattern" >&2
    exit 1
  fi
}

root_html="$(fetch "${shop_base_url}/")"
assert_contains "$root_html" '<!DOCTYPE html|<html' 'home'

products_html="$(fetch "${shop_base_url}/products")"
assert_contains "$products_html" 'Explore products|catalog|products' 'products'

login_html="$(fetch "${shop_base_url}/login")"
assert_contains "$login_html" 'Login|Sign in|Need the catalog first' 'login'

health_json="$(fetch "${shop_base_url}/api/health")"
assert_contains "$health_json" '"status"[[:space:]]*:[[:space:]]*"UP"' 'frontend health'
