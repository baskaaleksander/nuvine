# Keycloak Realm Setup Guide

This guide explains how to set up the Keycloak realm for the Nuvine platform.

## Overview

The `nuvine-keycloak.json` file contains the full realm export including:
- 6 service account clients with OAuth2 client credentials
- Realm roles (ROLE_USER, ROLE_ADMIN, ROLE_INTERNAL_SERVICE)
- Client roles and role mappings
- Client scopes and protocol mappers

## Quick Start

1. Copy the example file and add your secrets:
   ```bash
   cp nuvine-keycloak.example.json nuvine-keycloak.json
   ```

2. Generate secrets for each client (or use your own):
   ```bash
   # Generate a secure random secret
   openssl rand -base64 32
   ```

3. Replace the placeholder secrets in `nuvine-keycloak.json` (see table below)

4. Update your `.env` file with matching secrets

5. Start the stack:
   ```bash
   cd infra/docker
   docker compose up -d
   ```

## Client Secrets Configuration

The following clients require secrets to be configured. The secrets in `nuvine-keycloak.json` must match the corresponding environment variables in your `.env` file.

| Line | Client ID | JSON Field | .env Variable |
|------|-----------|------------|---------------|
| 880 | `chat-service` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_CHAT_SERVICE_SECRET` |
| 939 | `file-storage-service` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_FILE_STORAGE_SERVICE_SECRET` |
| 1000 | `ingestion-service` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_INGESTION_SERVICE_SECRET` |
| 1059 | `nuvine-app` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_CLIENT_SECRET` |
| 1229 | `subscription-service` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_SUBSCRIPTION_SERVICE_SECRET` |
| 1288 | `workspace-service` | `"secret": "CHANGE_ME_..."` | `KEYCLOAK_WORKSPACE_SERVICE_SECRET` |

## Secret Format Requirements

- **Minimum length**: 32 characters recommended
- **Character set**: Alphanumeric (a-z, A-Z, 0-9) and special characters
- **Generation**: Use `openssl rand -base64 32` for secure random secrets

## Example .env Configuration

```env
# Keycloak Client Secrets
# These MUST match the secrets in nuvine-keycloak.json
KEYCLOAK_CLIENT_SECRET=your-nuvine-app-secret-here
KEYCLOAK_CHAT_SERVICE_SECRET=your-chat-service-secret-here
KEYCLOAK_FILE_STORAGE_SERVICE_SECRET=your-file-storage-secret-here
KEYCLOAK_INGESTION_SERVICE_SECRET=your-ingestion-service-secret-here
KEYCLOAK_SUBSCRIPTION_SERVICE_SECRET=your-subscription-secret-here
KEYCLOAK_WORKSPACE_SERVICE_SECRET=your-workspace-service-secret-here
```

## Service Account Clients

| Client ID | Has Service Account | Purpose |
|-----------|---------------------|---------|
| `nuvine-app` | Yes | Main application client with realm-admin privileges |
| `chat-service` | Yes | Internal service-to-service communication |
| `file-storage-service` | Yes | Internal service-to-service communication |
| `ingestion-service` | Yes | Internal service-to-service communication |
| `subscription-service` | Yes | Internal service-to-service communication |
| `workspace-service` | No | Resource server only (validates tokens) |

## Realm Import Behavior

Keycloak's `--import-realm` flag:
- Only imports if the realm does **not** exist
- Will **not** overwrite an existing realm
- To re-import, delete the Keycloak volume first:
  ```bash
  docker compose down
  docker volume rm docker_nuvine_keycloak
  docker compose up -d
  ```

## Files

| File | Purpose | Git Status |
|------|---------|------------|
| `nuvine-keycloak.json` | Actual realm config with real secrets | **Ignored** (contains secrets) |
| `nuvine-keycloak.example.json` | Template with placeholder secrets | Committed |
| `KEYCLOAK_SETUP.md` | This documentation | Committed |

## Troubleshooting

### Realm not importing
- Check if realm already exists in Keycloak admin console
- Delete the volume and restart: `docker volume rm docker_nuvine_keycloak`

### Authentication failures
- Verify secrets match between `nuvine-keycloak.json` and `.env`
- Check service logs for specific error messages
- Ensure Keycloak is fully started before services attempt authentication

### Service-to-service auth not working
- Verify the service has `serviceAccountsEnabled: true` in the realm config
- Check that the service account user has the required roles
- Confirm the `ROLE_INTERNAL_SERVICE` role is assigned
