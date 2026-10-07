ALTER TABLE vehiculos
    DROP CONSTRAINT IF EXISTS chk_vehiculos_estado;

UPDATE vehiculos
SET estado = CASE UPPER(BTRIM(estado))
                 WHEN 'DISPONIBLE' THEN 'DISPONIBLE'
                 WHEN 'VENDIDO' THEN 'VENDIDO'
                 WHEN 'EN MANTENIMIENTO' THEN 'EN_MANTENIMIENTO'
                 WHEN 'EN_MANTENIMIENTO' THEN 'EN_MANTENIMIENTO'
                 ELSE 'DISPONIBLE'
             END;

ALTER TABLE vehiculos
    ADD CONSTRAINT chk_vehiculos_estado
    CHECK (estado IN ('DISPONIBLE', 'VENDIDO', 'EN_MANTENIMIENTO'));

ALTER TABLE clientes      ALTER COLUMN nombre   TYPE VARCHAR(80);
ALTER TABLE clientes      ALTER COLUMN apellido TYPE VARCHAR(80);
ALTER TABLE clientes      ALTER COLUMN email    TYPE VARCHAR(160);
ALTER TABLE clientes      ALTER COLUMN telefono TYPE VARCHAR(30);
ALTER TABLE vehiculos     ALTER COLUMN placa    TYPE VARCHAR(12);
ALTER TABLE vehiculos     ALTER COLUMN marca    TYPE VARCHAR(60);
ALTER TABLE vehiculos     ALTER COLUMN modelo   TYPE VARCHAR(80);
ALTER TABLE mantenimientos ALTER COLUMN descripcion TYPE VARCHAR(500);
