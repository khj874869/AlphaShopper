#!/usr/bin/env bash

run_common_preflight_checks() {
  local repo_root="$1"
  local frontend_dir="$2"

  if ! bash "$repo_root/scripts/verify-toolchain-versions.sh" >/dev/null; then
    errors+=("Toolchain version alignment failed.")
  fi

  if ! command -v docker >/dev/null 2>&1; then
    errors+=("docker CLI is not available.")
  elif ! docker compose version >/dev/null 2>&1; then
    errors+=("Docker Compose is not available.")
  fi

  local frontend_node_modules_dir="$frontend_dir/node_modules"
  if [[ ! -d "$frontend_node_modules_dir" ]]; then
    warnings+=("frontend/node_modules is missing. Run 'cd frontend && npm ci' before starting the frontend.")
  fi
}

print_preflight_warnings() {
  if (( ${#warnings[@]} > 0 )); then
    printf '\nWarnings:\n'
    for warning in "${warnings[@]}"; do
      printf ' - %s\n' "$warning"
    done
  fi
}

print_preflight_warnings_stderr() {
  if (( ${#warnings[@]} > 0 )); then
    printf '\nWarnings:\n' >&2
    for warning in "${warnings[@]}"; do
      printf ' - %s\n' "$warning" >&2
    done
  fi
}
