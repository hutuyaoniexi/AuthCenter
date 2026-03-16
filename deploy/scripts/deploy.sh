#!/usr/bin/env bash
set -euo pipefail

: "${IMAGE_NAME:?IMAGE_NAME is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"
: "${DOCKER_COMPOSE_FILE:?DOCKER_COMPOSE_FILE is required}"

IMAGE_FULL="${IMAGE_NAME}:${IMAGE_TAG}"
STATE_DIR="$(dirname "${DOCKER_COMPOSE_FILE}")/.deploy-meta"
CURRENT_FILE="${STATE_DIR}/current_version"
PREVIOUS_FILE="${STATE_DIR}/previous_version"

mkdir -p "${STATE_DIR}"

echo "Deploy image: ${IMAGE_FULL}"
echo "Compose file: ${DOCKER_COMPOSE_FILE}"

# 记录前一个版本
if [[ -f "${CURRENT_FILE}" ]]; then
    CURRENT_TAG="$(cat "${CURRENT_FILE}" || true)"
    if [[ -n "${CURRENT_TAG}" && "${CURRENT_TAG}" != "${IMAGE_TAG}" ]]; then
        echo "${CURRENT_TAG}" > "${PREVIOUS_FILE}"
        echo "Previous version recorded: ${CURRENT_TAG}"
    fi
fi

# 如果是远程仓库镜像（一般包含 registry/namespace/repo），先拉取
# 本地模式 IMAGE_NAME=admin-system，不会包含 /
if [[ "${IMAGE_NAME}" == */* ]]; then
    echo "Detected registry image, pulling ${IMAGE_FULL} ..."
    docker pull "${IMAGE_FULL}"
else
    echo "Detected local image, skip pull."
fi

export IMAGE_NAME
export IMAGE_TAG

docker compose -f "${DOCKER_COMPOSE_FILE}" down || true
docker compose -f "${DOCKER_COMPOSE_FILE}" up -d

echo "${IMAGE_TAG}" > "${CURRENT_FILE}"
echo "Current version recorded: ${IMAGE_TAG}"

docker ps
