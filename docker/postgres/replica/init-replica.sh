#!/bin/bash
# Script executado em cada réplica.
# Faz um pg_basebackup do primário e inicia como standby.

set -e

PRIMARY_HOST="postgres-primary"
PRIMARY_PORT="5432"
PGDATA="/var/lib/postgresql/data"

echo ">>> Aguardando primário ficar disponível..."
until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$PGUSER"; do
  sleep 2
done

echo ">>> Primário disponível. Iniciando pg_basebackup..."

rm -rf "$PGDATA"/*

PGPASSWORD="$PGPASSWORD" pg_basebackup \
  -h "$PRIMARY_HOST" \
  -p "$PRIMARY_PORT" \
  -U "$PGUSER" \
  -D "$PGDATA" \
  -Fp \
  -Xs \
  -P \
  -R

echo ">>> pg_basebackup concluído. A réplica está configurada como standby."
echo ">>> Iniciando PostgreSQL como réplica..."

exec docker-entrypoint.sh postgres
