-- Debezium читает изменения таблицы outbox через эту publication (ADR-006).
-- Создаём её миграцией от имени владельца таблицы: у пользователя debezium прав на это нет и не должно быть.
CREATE PUBLICATION dbz_profile_outbox FOR TABLE outbox_events;
