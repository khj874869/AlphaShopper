#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

expect_equal() {
  local label="$1"
  local actual="$2"
  local expected="$3"

  if [[ "$actual" != "$expected" ]]; then
    fail "$label mismatch. expected='$expected' actual='$actual'"
  fi
}

node_version="$(tr -d '\r\n' < .nvmrc)"
java_version="$(sed -n 's:.*<java.version>\(.*\)</java.version>.*:\1:p' pom.xml | head -n 1)"

[[ -n "$node_version" ]] || fail "Unable to read Node version from .nvmrc"
[[ -n "$java_version" ]] || fail "Unable to read Java version from pom.xml"

node_major="${node_version%%.*}"
next_node_major="$((node_major + 1))"
expected_node_engine=">=${node_version} <${next_node_major}"
package_node_engine="$(sed -n 's/.*"node":[[:space:]]*"\([^"]*\)".*/\1/p' frontend/package.json | head -n 1)"

expect_equal "frontend/package.json engines.node" "$package_node_engine" "$expected_node_engine"

frontend_docker_versions="$(sed -n 's/^FROM node:\([^ ]*\).*/\1/p' frontend/Dockerfile | sort -u)"
expect_equal "frontend/Dockerfile Node base image" "$frontend_docker_versions" "${node_version}-alpine"

backend_build_java="$(sed -n 's/^FROM maven:.*-eclipse-temurin-\([0-9][0-9]*\) AS build$/\1/p' Dockerfile | head -n 1)"
backend_runtime_java="$(sed -n 's/^FROM eclipse-temurin:\([0-9][0-9]*\)-jre$/\1/p' Dockerfile | head -n 1)"
expect_equal "Dockerfile build Java" "$backend_build_java" "$java_version"
expect_equal "Dockerfile runtime Java" "$backend_runtime_java" "$java_version"

workflow_java_versions="$(sed -n 's/.*java-version: "\([0-9][0-9]*\)"/\1/p' .github/workflows/*.yml | sort -u)"
expect_equal "GitHub Actions Java version" "$workflow_java_versions" "$java_version"

workflow_node_version_files="$(sed -n 's/.*node-version-file: \(.*\)$/\1/p' .github/workflows/*.yml | tr -d '"' | sort -u)"
expect_equal "GitHub Actions node-version-file" "$workflow_node_version_files" ".nvmrc"

echo "Toolchain versions are aligned."
