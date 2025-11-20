#!/bin/bash

# -----------------------------------------
# Load .env variables
# -----------------------------------------

if [ -f ../.env ]; then
  set -a
  source ../.env
  set +a
else
  echo "../.env file not found!"
  exit 1
fi

# -----------------------------------------
# Config
# -----------------------------------------

MINIO_ALIAS="local"
BUCKET_NAME="nuvine"
WEBHOOK_NAME="filestorage"

WEBHOOK_ENDPOINT="http://host.docker.internal:8222/api/v1/internal/file-storage/events"

echo "[1/3] adding webhook: $WEBHOOK_NAME"

mc admin config set ${MINIO_ALIAS} notify_webhook:${WEBHOOK_NAME} \
  endpoint="${WEBHOOK_ENDPOINT}" \
  queue_limit="10000" \
  queue_dir="/tmp/minio-events" \
  max_retry="5" \
  authorization="Bearer ${MINIO_WEBHOOK_SECRET}" >/dev/null

if [ $? -ne 0 ]; then
  echo "failed during configuration!"
  exit 1
fi

echo "webhook configured."

echo "[2/3] reloading MinIO config"

mc admin service restart ${MINIO_ALIAS} >/dev/null

if [ $? -ne 0 ]; then
  echo "service restart failed"
  exit 1
fi

echo "service restarted"

echo "[3/3] assigning PUT event to bucket: ${BUCKET_NAME}"

mc event add ${MINIO_ALIAS}/${BUCKET_NAME} \
  arn:minio:sqs::${WEBHOOK_NAME}:webhook \
  --event put >/dev/null

if [ $? -ne 0 ]; then
  echo "failed during event assignment!"
  exit 1
fi

echo "ready. PUT events for bucket '${BUCKET_NAME}' will be sent to:"
echo "${WEBHOOK_ENDPOINT}"
echo "using Authorization: Bearer ${MINIO_WEBHOOK_SECRET}"
