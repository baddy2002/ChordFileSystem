#!/bin/bash
set -e
docker build -t chord-node -f docker/Dockerfile .
docker compose -f docker/docker-compose.yml up -d "$@"