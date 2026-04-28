#!/usr/bin/env bash

prepare_frontend_env_file() {
  local source_env_file="$1"
  local target_env_file="$2"

  if [[ ! -f "$target_env_file" ]]; then
    cp "$source_env_file" "$target_env_file"
    printf 'Copied frontend env file to: %s\n' "$target_env_file"
  else
    printf 'Using existing frontend env file: %s\n' "$target_env_file"
  fi
}

require_frontend_node_modules() {
  local node_modules_dir="$1"

  if [[ ! -d "$node_modules_dir" ]]; then
    printf "frontend/node_modules is missing. Run 'cd frontend && npm ci' first.\n" >&2
    exit 1
  fi
}
