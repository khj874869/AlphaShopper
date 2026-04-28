#!/usr/bin/env bash

resolve_preferred_env_file() {
  local preferred_path="$1"
  local fallback_path="$2"

  if [[ -f "$preferred_path" ]]; then
    printf '%s\n' "$preferred_path"
    return
  fi

  if [[ -f "$fallback_path" ]]; then
    printf '%s\n' "$fallback_path"
    return
  fi

  printf 'Neither env file exists: %s or %s\n' "$preferred_path" "$fallback_path" >&2
  exit 1
}

import_dotenv_file() {
  local path="$1"
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"

    [[ -z "$line" || "${line:0:1}" == "#" ]] && continue
    [[ "$line" != *=* ]] && continue

    local name="${line%%=*}"
    local value="${line#*=}"
    export "$name=$value"
  done < "$path"
}
