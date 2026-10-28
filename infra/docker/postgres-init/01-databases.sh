#!/bin/bash
# Создаёт отдельную БД и пользователя на каждый сервис.
# Выполняется один раз — при первом старте контейнера с пустым томом.
set -euo pipefail

create_database() {
    local db="$1" user="$2" password="$3"
    echo "Creating database '$db' owned by '$user'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
        CREATE USER "$user" WITH PASSWORD '$password';
        CREATE DATABASE "$db" OWNER "$user";
EOSQL
}

create_database keycloak       keycloak "$KEYCLOAK_DB_PASSWORD"
create_database lumeva_profile profile  "$PROFILE_DB_PASSWORD"

# Debezium читает журнал изменений (WAL), поэтому ему нужна роль REPLICATION.
# Права только на чтение: писать в базы сервисов он не должен.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER debezium WITH REPLICATION PASSWORD '$DEBEZIUM_DB_PASSWORD';
    GRANT CONNECT ON DATABASE lumeva_profile TO debezium;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname lumeva_profile <<-EOSQL
    GRANT USAGE ON SCHEMA public TO debezium;
    -- таблицы создаст Liquibase от имени profile; заранее выдаём чтение на будущие таблицы
    ALTER DEFAULT PRIVILEGES FOR ROLE profile IN SCHEMA public GRANT SELECT ON TABLES TO debezium;
EOSQL
