#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"
backend_env_file="$repo_root/.env.toss.local"
frontend_env_file="$frontend_dir/.env.toss.local"
backend_env_fallback="$repo_root/.env.toss.local.example"
frontend_env_fallback="$frontend_dir/.env.toss.local.example"

# Reuse the same env-file parsing rules across Bash launchers.
source "$repo_root/scripts/import-dotenv.sh"
source "$repo_root/scripts/preflight-common.sh"

backend_env_file="$(resolve_preferred_env_file "$backend_env_file" "$backend_env_fallback")"
frontend_env_file="$(resolve_preferred_env_file "$frontend_env_file" "$frontend_env_fallback")"

import_dotenv_file "$backend_env_file"
import_dotenv_file "$frontend_env_file"

errors=()
warnings=()

run_common_preflight_checks "$repo_root" "$frontend_dir"

[[ "${APP_PAYMENT_PROVIDER:-}" == "toss" ]] || errors+=("APP_PAYMENT_PROVIDER must be toss.")
[[ -n "${APP_PAYMENT_TOSS_SECRET_KEY:-}" && "${APP_PAYMENT_TOSS_SECRET_KEY:-}" != test_sk_your_* ]] || errors+=("APP_PAYMENT_TOSS_SECRET_KEY must be replaced with a real Toss test secret key.")
[[ "${SPRING_PROFILES_ACTIVE:-}" == *toss-local* ]] || errors+=("SPRING_PROFILES_ACTIVE should include toss-local.")
[[ "${NEXT_PUBLIC_PAYMENT_PROVIDER:-}" == "toss" ]] || errors+=("NEXT_PUBLIC_PAYMENT_PROVIDER must be toss.")
[[ "${APP_FRONTEND_BASE_URL:-}" == "http://localhost:3000" ]] || errors+=("APP_FRONTEND_BASE_URL should be http://localhost:3000 for local Toss tests.")

if (( ${#errors[@]} > 0 )); then
  printf 'Toss local preflight failed:\n' >&2
  for error in "${errors[@]}"; do
    printf ' - %s\n' "$error" >&2
  done
  print_preflight_warnings_stderr
  exit 1
fi

printf 'Toss local preflight passed.\n'
printf 'Backend env file : %s\n' "$backend_env_file"
printf 'Frontend env file: %s\n' "$frontend_env_file"
printf 'Profiles         : %s\n' "${SPRING_PROFILES_ACTIVE:-}"
printf 'API base URL     : %s\n' "${NEXT_PUBLIC_API_BASE_URL:-}"
printf 'Storefront URL   : %s\n' "${APP_FRONTEND_BASE_URL:-}"
print_preflight_warnings

printf '\nRecommended next steps:\n'
printf '  1. docker compose up -d\n'
printf '  2. bash scripts/start-toss-local-backend.sh\n'
printf '  3. bash scripts/start-toss-local-frontend.sh\n'
