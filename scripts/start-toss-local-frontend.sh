#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"
env_file="$frontend_dir/.env.toss.local"
fallback_env_file="$frontend_dir/.env.toss.local.example"
target_env_file="$frontend_dir/.env.local"
node_modules_dir="$frontend_dir/node_modules"

source "$repo_root/scripts/import-dotenv.sh"
source "$repo_root/scripts/frontend-dev.sh"

env_file="$(resolve_preferred_env_file "$env_file" "$fallback_env_file")"
prepare_frontend_env_file "$env_file" "$target_env_file"
require_frontend_node_modules "$node_modules_dir"

printf 'Starting Next.js dev server with Toss local settings\n'

cd "$frontend_dir"
npm run dev
