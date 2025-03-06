CREATE EXTENSION IF NOT EXISTS dblink;

CREATE OR REPLACE PROCEDURE sp_create_database(p_dbname VARCHAR)
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_exec(
        'host=localhost dbname=postgres user=' || current_user,
        'CREATE DATABASE ' || quote_ident(p_dbname)
    );
    RAISE NOTICE 'Database "%" created.', p_dbname;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_drop_database(p_dbname VARCHAR)
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_exec(
        'host=localhost dbname=postgres user=' || current_user,
        'DO $inner$
         BEGIN
           PERFORM pg_terminate_backend(pid)
           FROM pg_stat_activity
           WHERE datname = ' || quote_literal(p_dbname) || ' AND pid <> pg_backend_pid();
         END $inner$;'
    );
    PERFORM dblink_exec(
        'host=localhost dbname=postgres user=' || current_user,
        'DROP DATABASE IF EXISTS ' || quote_ident(p_dbname)
    );
    RAISE NOTICE 'Database "%" dropped.', p_dbname;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_create_table(p_tablename VARCHAR)
LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND lower(table_name) = lower(p_tablename)
    ) THEN
         RAISE NOTICE 'Table "%" already exists.', p_tablename;
    ELSE
         EXECUTE format('CREATE TABLE %I (
             id SERIAL PRIMARY KEY,
             surname VARCHAR(255),
             name VARCHAR(255),
             lastName VARCHAR(255),
             "group" INT
         )', p_tablename);
         RAISE NOTICE 'Table "%" created.', p_tablename;
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_clear_table(p_tablename VARCHAR)
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format('TRUNCATE TABLE %I', p_tablename);
    RAISE NOTICE 'Table "%" cleared.', p_tablename;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_add_child(
    p_tablename VARCHAR,
    p_surname VARCHAR,
    p_name VARCHAR,
    p_lastName VARCHAR,
    p_group INT
)
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format(
      'INSERT INTO %I (surname, name, lastName, "group") VALUES (%L, %L, %L, %s)',
      p_tablename, p_surname, p_name, p_lastName, p_group
    );
    RAISE NOTICE 'Child added: %', p_surname;
END;
$$;

CREATE OR REPLACE FUNCTION sp_search_child_by_surname(p_tablename VARCHAR, p_surname VARCHAR)
RETURNS TABLE(
    id INT,
    p_surname VARCHAR,
    p_name VARCHAR,
    p_lastName VARCHAR,
    p_group INT
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public'
          AND lower(table_name) = lower(p_tablename)
    ) THEN
        RETURN;
    ELSE
        RETURN QUERY EXECUTE format(
            'SELECT id, surname, name, lastName, "group" FROM %I WHERE surname ILIKE %L',
            p_tablename, '%' || p_surname || '%'
        );
    END IF;
END;
$$;


CREATE OR REPLACE PROCEDURE sp_update_child(
    p_tablename VARCHAR,
    p_id INT,
    p_surname VARCHAR,
    p_name VARCHAR,
    p_lastName VARCHAR,
    p_group INT
)
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format(
      'UPDATE %I SET surname=%L, name=%L, lastName=%L, "group"=%s WHERE id=%s',
      p_tablename, p_surname, p_name, p_lastName, p_group, p_id
    );
    RAISE NOTICE 'Child updated with id: %', p_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_delete_child_by_surname(p_tablename VARCHAR, p_surname VARCHAR)
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format(
      'DELETE FROM %I WHERE surname ILIKE %L',
      p_tablename, p_surname
    );
    RAISE NOTICE 'Child(ren) with surname "%" deleted.', p_surname;
END;
$$;