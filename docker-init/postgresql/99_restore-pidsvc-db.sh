#!/bin/sh
set -e

POSTGRES_DB="${POSTGRES_DB:-}"
if [ -z "${POSTGRES_DB}" ]; then
    echo "POSTGRES_DB is not set: not attempting to pg_restore anything."
    return
fi

POSTGRES_DUMP_FILE="${POSTGRES_DUMP_FILE:-}"
if [ -z "${POSTGRES_DUMP_FILE}" ]; then
    echo "POSTGRES_DUMP_FILE is not set: not attempting to pg_restore anything."
    return
fi

POSTGRES_DUMP_PATH="/docker-entrypoint-initdb.d/${POSTGRES_DUMP_FILE}"
if [ ! -e "${POSTGRES_DUMP_PATH}" ]; then
    echo "POSTGRES_DUMP_FILE '${POSTGRES_DUMP_PATH}' not found: can't pg_restore it"
    exit 1
fi

case "${POSTGRES_DUMP_PATH}" in
    *.gz) gunzip -c "${POSTGRES_DUMP_PATH}" | pg_restore \
            --dbname="${POSTGRES_DB}" \
            --username="${POSTGRES_USER:-postgres}" \
            --format="${POSTGRES_DUMP_FORMAT:-tar}" \
            --clean --if-exists \
            --no-owner \
            --verbose ;;
    *) pg_restore \
            --dbname="${POSTGRES_DB}" \
            --username="${POSTGRES_USER:-postgres}" \
            --format="${POSTGRES_DUMP_FORMAT:-tar}" \
            --clean --if-exists \
            --no-owner \
            --verbose \
            "${POSTGRES_DUMP_PATH}" ;;
esac
