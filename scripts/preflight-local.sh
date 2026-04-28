#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"
frontend_env_file="$frontend_dir/.env.local"
frontend_env_example_file="$frontend_dir/.env.local.example"

source "$repo_root/scripts/preflight-common.sh"

errors=()
warnings=()

run_common_preflight_checks "$repo_root" "$frontend_dir"

if [[ ! -f "$frontend_env_file" ]]; then
  if [[ -f "$frontend_env_example_file" ]]; then
    warnings+=("frontend/.env.local is missing. start-local-frontend.sh will copy .env.local.example automatically.")
  else
    errors+=("frontend/.env.local and frontend/.env.local.example are both missing.")
  fi
fi

if (( ${#errors[@]} > 0 )); then
  printf 'Local preflight failed:\n' >&2
  for error in "${errors[@]}"; do
    printf ' - %s\n' "$error" >&2
  done
  print_preflight_warnings_stderr
  exit 1
fi

printf 'Local preflight passed.\n'
print_preflight_warnings

printf '\nRecommended next steps:\n'
printf '  1. docker compose up -d\n'
printf '  2. bash scripts/start-local-backend.sh\n'
printf '  3. bash scripts/start-local-frontend.sh\n'
