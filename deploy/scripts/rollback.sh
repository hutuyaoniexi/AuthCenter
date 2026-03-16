#!/usr/bin/env bash
set -euo pipefail

: "${IMAGE_NAME:?IMAGE_NAME is required}"
: "${DOCKER_COMPOSE_FILE:?DOCKER_COMPOSE_FILE is required}"

STATE_DIR="$(dirname "${DOCKER_COMPOSE_FILE}")/.deploy-meta"
CURRENT_FILE="${STATE_DIR}/current_version"
PREVIOUS_FILE="${STATE_DIR}/previous_version"

mkdir -p "${STATE_DIR}"

TARGET_TAG=""

# 1. 优先使用外部传入版本
if [[ -n "${ROLLBACK_TAG:-}" ]]; then
    TARGET_TAG="${ROLLBACK_TAG}"
    echo "Use rollback tag from parameter: ${TARGET_TAG}"
fi

# 2. 否则优先用 previous_version
if [[ -z "${TARGET_TAG}" && -f "${PREVIOUS_FILE}" ]]; then
    TARGET_TAG="$(cat "${PREVIOUS_FILE}" || true)"
    if [[ -n "${TARGET_TAG}" ]]; then
        echo "Use rollback tag from previous_version: ${TARGET_TAG}"
    fi
fi

# 3. 再兜底：根据本地镜像 tag 推断上一个版本
if [[ -z "${TARGET_TAG}" ]]; then
    TARGET_TAG="$(
        docker images "${IMAGE_NAME}" --format "{{.Tag}}" \
        | grep -E '^[0-9]+$' \
        | sort -nr \
        | sed -n '2p'
    )"
    if [[ -n "${TARGET_TAG}" ]]; then
        echo "Use rollback tag from local image history: ${TARGET_TAG}"
    fi
fi

if [[ -z "${TARGET_TAG}" ]]; then
    echo "未找到可回滚的版本"
    exit 1
fi

IMAGE_FULL="${IMAGE_NAME}:${TARGET_TAG}"
echo "Rollback to ${IMAGE_FULL}"

# registry 镜像先 pull
if [[ "${IMAGE_NAME}" == */* ]]; then
    echo "Detected registry image, pulling ${IMAGE_FULL} ..."
    docker pull "${IMAGE_FULL}"
else
    echo "Detected local image, skip pull."
fi

export IMAGE_TAG="${TARGET_TAG}"

docker compose -f "${DOCKER_COMPOSE_FILE}" down || true
docker compose -f "${DOCKER_COMPOSE_FILE}" up -d

# 更新 current_version
if [[ -f "${CURRENT_FILE}" ]]; then
    OLD_CURRENT="$(cat "${CURRENT_FILE}" || true)"
    if [[ -n "${OLD_CURRENT}" && "${OLD_CURRENT}" != "${TARGET_TAG}" ]]; then
        echo "${OLD_CURRENT}" > "${PREVIOUS_FILE}"
    fi
fi

echo "${TARGET_TAG}" > "${CURRENT_FILE}"
echo "Current version updated: ${TARGET_TAG}"

docker ps
