#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"
example_env_file="$frontend_dir/.env.local.example"
target_env_file="$frontend_dir/.env.local"
node_modules_dir="$frontend_dir/node_modules"

source "$repo_root/scripts/frontend-dev.sh"

prepare_frontend_env_file "$example_env_file" "$target_env_file"
require_frontend_node_modules "$node_modules_dir"

printf 'Starting Next.js dev server with local settings\n'

cd "$frontend_dir"
npm run dev
