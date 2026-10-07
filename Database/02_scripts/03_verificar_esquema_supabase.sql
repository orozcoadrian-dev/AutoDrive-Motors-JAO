WITH esperado (tabla, columna, tipos_validos, admite_nulo, nota) AS (
    VALUES
        ('clientes', 'id',             ',int8,bigint,',                                                        false, 'PK identidad'),
        ('clientes', 'nombre',         ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(80)'),
        ('clientes', 'apellido',       ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(80)'),
        ('clientes', 'documento',      ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(30) único - migración V3'),
        ('clientes', 'email',          ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(160) único'),
        ('clientes', 'telefono',       ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(30)'),
        ('clientes', 'fecha_registro', ',timestamp,timestampwithouttimezone,timestamptz,timestampwithtimezone,', false, 'LocalDateTime'),
        ('vehiculos', 'id',            ',int8,bigint,',                                                        false, 'PK identidad'),
        ('vehiculos', 'placa',         ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(12) único'),
        ('vehiculos', 'marca',         ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(60)'),
        ('vehiculos', 'modelo',        ',varchar,charactervarying,bpchar,text,',                               false, 'VARCHAR(80)'),
        ('vehiculos', 'anio',          ',int2,int4,int8,smallint,integer,bigint,numeric,',                     false, 'Integer'),
        ('vehiculos', 'precio_cop',    ',numeric,decimal,',                                                    false, 'NUMERIC(15,2)'),
        ('vehiculos', 'estado',        ',varchar,charactervarying,bpchar,text,',                               false, 'DISPONIBLE | VENDIDO | EN_MANTENIMIENTO'),
        ('vehiculos', 'version',       ',int2,int4,int8,smallint,integer,bigint,',                             false, '@Version - migración V5'),
        ('ventas', 'id',                  ',int8,bigint,',                                                     false, 'PK identidad'),
        ('ventas', 'cliente_id',          ',int8,bigint,',                                                     false, 'FK clientes(id)'),
        ('ventas', 'vehiculo_id',         ',int8,bigint,',                                                     false, 'FK vehiculos(id), UNIQUE'),
        ('ventas', 'fecha_venta',         ',timestamp,timestampwithouttimezone,timestamptz,timestampwithtimezone,', false, 'LocalDateTime'),
        ('ventas', 'descuento_aplicado',  ',numeric,decimal,',                                                 false, 'NUMERIC(15,2)'),
        ('ventas', 'monto_total',         ',numeric,decimal,',                                                 false, 'NUMERIC(15,2)'),
        ('mantenimientos', 'id',                  ',int8,bigint,',                                             false, 'PK identidad'),
        ('mantenimientos', 'vehiculo_id',         ',int8,bigint,',                                             false, 'FK vehiculos(id)'),
        ('mantenimientos', 'fecha_mantenimiento', ',date,',                                                    false, 'LocalDate - migración V4'),
        ('mantenimientos', 'descripcion',         ',varchar,charactervarying,bpchar,text,',                    false, 'VARCHAR(500)'),
        ('mantenimientos', 'costo',               ',numeric,decimal,',                                         false, 'NUMERIC(15,2)')
),
comparacion AS (
    SELECT e.tabla,
           e.columna,
           e.nota,
           c.data_type,
           c.is_nullable,
           CASE
               WHEN c.column_name IS NULL THEN 'FALTA'
               WHEN UPPER(e.tipos_validos) NOT LIKE
                    '%,' || REPLACE(UPPER(COALESCE(c.data_type, '')), ' ', '') || ',%' THEN 'TIPO'
               WHEN e.admite_nulo = false AND c.is_nullable = 'YES' THEN 'NULABLE'
               ELSE 'OK'
           END AS estado
    FROM esperado e
    LEFT JOIN information_schema.columns c
           ON UPPER(c.table_schema) = UPPER('public')
          AND UPPER(c.table_name) = UPPER(e.tabla)
          AND UPPER(c.column_name) = UPPER(e.columna)
),
tablas_esperadas (tabla, columnas) AS (
    VALUES ('clientes', 7), ('vehiculos', 8), ('ventas', 6), ('mantenimientos', 5)
),
resumen_tablas AS (
    SELECT t.tabla,
           t.columnas AS columnas_esperadas,
           COUNT(c.column_name) AS columnas_presentes,
           CASE WHEN COUNT(c.column_name) = t.columnas THEN 'OK' ELSE 'INCOMPLETA' END AS estado
    FROM tablas_esperadas t
    LEFT JOIN information_schema.columns c
           ON UPPER(c.table_schema) = UPPER('public')
          AND UPPER(c.table_name) = UPPER(t.tabla)
    GROUP BY t.tabla, t.columnas
)
SELECT tabla,
       columna,
       nota                       AS esperado_por_jpa,
       COALESCE(data_type, '-')   AS tipo_actual,
       COALESCE(is_nullable, '-') AS nulo_actual,
       estado
FROM comparacion
WHERE estado <> 'OK'

UNION ALL

SELECT tabla,
       'TABLA',
       'columnas esperadas: ' || columnas_esperadas,
       'presentes: ' || columnas_presentes,
       '-',
       'REVISAR'
FROM resumen_tablas
WHERE estado <> 'OK'

UNION ALL

SELECT 'flyway_schema_history',
       'version',
       'migraciones aplicadas en la base (se espera 5)',
       COALESCE(MAX(version), 'ninguna aplicada'),
       '-',
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.tables
                  WHERE UPPER(table_schema) = UPPER('public')
                    AND UPPER(table_name) = UPPER('flyway_schema_history')) = 0 THEN 'REVISAR'
           WHEN MAX(version) IS NULL THEN 'REVISAR'
           WHEN MAX(version) LIKE '5%' THEN 'OK'
           ELSE 'REVISAR'
       END
FROM flyway_schema_history

ORDER BY tabla, columna;
