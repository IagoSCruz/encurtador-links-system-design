#!/bin/bash
# Script executado no PostgreSQL primário na primeira inicialização.
# Cria o usuário de replicação e configura permissões.

set -e

echo ">>> Configurando usuário de replicação no primário..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Cria usuário de replicação
    CREATE ROLE replicador WITH REPLICATION LOGIN PASSWORD 'replicador123';
    GRANT CONNECT ON DATABASE encurtador TO replicador;
EOSQL

echo ">>> Usuário de replicação criado com sucesso."
