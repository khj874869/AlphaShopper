#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="$repo_root/.env.toss.local"
fallback_env_file="$repo_root/.env.toss.local.example"

source "$repo_root/scripts/import-dotenv.sh"

env_file="$(resolve_preferred_env_file "$env_file" "$fallback_env_file")"
import_dotenv_file "$env_file"

if [[ -z "${APP_PAYMENT_TOSS_SECRET_KEY:-}" || "${APP_PAYMENT_TOSS_SECRET_KEY:-}" == test_sk_your_* ]]; then
  printf 'APP_PAYMENT_TOSS_SECRET_KEY is missing. Set a real Toss test secret key in %s\n' "$env_file" >&2
  exit 1
fi

printf 'Using backend env file: %s\n' "$env_file"
printf 'Starting backend with profiles: %s\n' "${SPRING_PROFILES_ACTIVE:-}"

cd "$repo_root"
./mvnw spring-boot:run
