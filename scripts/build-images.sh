#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

target="${1:-all}"
backend_tag="${BACKEND_IMAGE_TAG:-zigzag-shop-backend:local}"
frontend_tag="${FRONTEND_IMAGE_TAG:-alphashopper-web:local}"

build_backend() {
  docker build -f Dockerfile -t "$backend_tag" .
}

build_frontend() {
  docker build -f frontend/Dockerfile -t "$frontend_tag" frontend
}

case "$target" in
  all)
    build_backend
    build_frontend
    ;;
  backend)
    build_backend
    ;;
  frontend)
    build_frontend
    ;;
  *)
    echo "Usage: $0 [all|backend|frontend]" >&2
    exit 1
    ;;
esac
