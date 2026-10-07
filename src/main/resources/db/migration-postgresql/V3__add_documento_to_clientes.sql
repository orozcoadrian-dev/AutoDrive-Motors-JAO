ALTER TABLE clientes
    ADD COLUMN documento VARCHAR(30);

UPDATE clientes
SET documento = 'LEGACY-' || id
WHERE documento IS NULL OR BTRIM(documento) = '';

ALTER TABLE clientes
    ALTER COLUMN documento SET NOT NULL;

ALTER TABLE clientes
    ADD CONSTRAINT uk_clientes_documento UNIQUE (documento);
