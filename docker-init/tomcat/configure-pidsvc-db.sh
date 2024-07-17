#!/bin/bash
set -e

PIDSVC_CONTEXT_XML="${CATALINA_HOME}/conf/Catalina/localhost/pidsvc.xml"
echo "Updating PIDSvc database context settings at '${PIDSVC_CONTEXT_XML}'"

# Update the database connection string
POSTGRES_HOST="${POSTGRES_HOST:-pidsvc-db}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-pidsvc}"
sed -i "s|url=\".*\"|url=\"jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}\"|" "${PIDSVC_CONTEXT_XML}"

# Update the database connection credentials username
POSTGRES_USER="${POSTGRES_USER:-pidsvc-admin}"
POSTGRES_USER_FILE="${POSTGRES_USER_FILE:-}"
if [[ -n "${POSTGRES_USER_FILE}" ]] && [[ -f "${POSTGRES_USER_FILE}" ]]; then
    POSTGRES_USER=$(cat "${POSTGRES_USER_FILE}" | xargs)
fi
sed -i "s|username=\".*\"|username=\"${POSTGRES_USER}\"|" "${PIDSVC_CONTEXT_XML}"

# Update the database connection credentials password
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-pidsvc123}"
POSTGRES_PASSWORD_FILE="${POSTGRES_PASSWORD_FILE:-}"
if [[ -n "${POSTGRES_PASSWORD_FILE}" ]] && [[ -f "${POSTGRES_PASSWORD_FILE}" ]]; then
    POSTGRES_PASSWORD=$(cat "${POSTGRES_PASSWORD_FILE}" | xargs)
fi
sed -i "s|password=\".*\"|password=\"${POSTGRES_PASSWORD}\"|" "${PIDSVC_CONTEXT_XML}"

echo "Updating PIDSvc database context settings at '${PIDSVC_CONTEXT_XML}' complete"
