-- Runs after the demo dump (PostgreSQL executes /docker-entrypoint-initdb.d in alphabetical order):
-- the restored tables have no statistics yet, and the first queries would otherwise plan badly.
ANALYZE;
