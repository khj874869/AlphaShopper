#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf 'Starting backend with local profile\n'

cd "$repo_root"
./mvnw spring-boot:run
