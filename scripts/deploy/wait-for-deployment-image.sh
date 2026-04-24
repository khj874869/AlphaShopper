#!/usr/bin/env bash
set -euo pipefail

: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

deployment_name="${1:?deployment name is required}"
container_name="${2:?container name is required}"
expected_image="${3:?expected image is required}"

for _ in $(seq 1 90); do
  current_image="$(
    kubectl get deployment "$deployment_name" -n "$K8S_NAMESPACE" \
      -o jsonpath="{range .spec.template.spec.containers[*]}{@.name}={@.image}{'\n'}{end}" \
      | awk -F= -v target="$container_name" '$1 == target { print $2 }'
  )"

  if [[ "$current_image" == "$expected_image" ]]; then
    exit 0
  fi

  sleep 10
done

echo "Timed out waiting for deployment/${deployment_name} container ${container_name} to use image ${expected_image}" >&2
exit 1
